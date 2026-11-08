package epn.edu.ec.service;

import epn.edu.ec.model.*;
import jakarta.persistence.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.logging.*;

public class ServicioCicloSalaPrivada {
    public record EstadoCiclo(
            UUID salaId,
            boolean iniciado,
            TipoBloque tipo,
            Instant inicioBloque,
            Instant finBloque,
            long restanteMillis,
            int orden,
            Instant horaServidor
    ) {}

    private final EntityManagerFactory factory;

    public ServicioCicloSalaPrivada(EntityManagerFactory factory) {
        this.factory = Objects.requireNonNull(factory);
    }

    public void iniciar(UUID responsableId, Instant ahora) {
        Instant inicio = normalizar(ahora);

        transaccion(em -> {
            var sala = salaDe(em, responsableId);

            if (!sala.getResponsableId().equals(responsableId)) {
                throw new SecurityException(
                        "Solo el responsable puede iniciar el estudio."
                );
            }

            var integrantes = participantes(em, sala);

            if (integrantes.size() < 2) {
                throw new IllegalStateException(
                        "Se necesitan al menos dos participantes para iniciar el estudio."
                );
            }
            if (integrantes.size() > PoliticaAdmisionSala.MAX_PARTICIPANTES) {
                throw new IllegalStateException(
                        "La sala supera el máximo de participantes."
                );
            }

            sala.getCiclo().iniciar(inicio);
            sala.actualizarBloqueActual(sala.getCiclo().getTipo());

            for (var participante : integrantes) {
                crearSesion(em, sala, participante, inicio);
            }

            return null;
        });
    }

    public void avanzar(UUID salaId, Instant ahora) {
        transaccion(em -> {
            var sala = em.find(
                    SalaPrivada.class,
                    Objects.requireNonNull(salaId),
                    LockModeType.PESSIMISTIC_WRITE
            );

            if (sala != null) {
                avanzarEnTransaccion(em, sala, ahora);
            }

            return null;
        });
    }

    public EstadoCiclo consultar(UUID estudianteId, Instant ahora) {
        Instant instante = normalizar(ahora);

        return transaccion(em -> {
            var sala = salaDe(em, estudianteId);

            avanzarEnTransaccion(em, sala, instante);

            var ciclo = sala.getCiclo();

            return new EstadoCiclo(
                    sala.getId(),
                    ciclo.isIniciado(),
                    ciclo.getTipo(),
                    ciclo.getInicioBloque(),
                    ciclo.getFinBloque(),
                    ciclo.tiempoRestante(instante).toMillis(),
                    ciclo.getOrden(),
                    instante
            );
        });
    }

    public void avanzarAbiertas(Instant ahora) {
        List<UUID> ids;

        try (var em = factory.createEntityManager()) {
            ids = em.createQuery(
                    "select s.id from SalaPrivada s "
                            + "where s.estado = :estado "
                            + "and s.ciclo.finBloque is not null",
                    UUID.class
            ).setParameter(
                    "estado",
                    SalaPrivada.Estado.ABIERTA
            ).getResultList();
        }

        for (UUID id : ids) {
            try {
                avanzar(id, ahora);
            } catch (RuntimeException error) {
                Logger.getLogger(
                        ServicioCicloSalaPrivada.class.getName()
                ).log(
                        Level.WARNING,
                        "No se pudo avanzar el ciclo privado " + id,
                        error
                );
            }
        }
    }

    // Se invoca dentro de una transacción con la sala bloqueada.
    public static void avanzarEnTransaccion(
            EntityManager em,
            SalaPrivada sala,
            Instant ahora
    ) {
        if (sala.getEstado() != SalaPrivada.Estado.ABIERTA
                || !sala.getCiclo().isIniciado()) {
            return;
        }

        Instant instante = normalizar(ahora);
        var ciclo = sala.getCiclo();
        var integrantes = participantes(em, sala);

        while (!instante.isBefore(ciclo.getFinBloque())) {
            Instant vencimiento = ciclo.getFinBloque();

            for (var participante : integrantes) {
                var personal = participante.getSesionPersonal();

                if (personal == null) {
                    throw new IllegalStateException(
                            "Falta la sesión personal de un participante."
                    );
                }

                personal.completarBloqueActual(vencimiento);
            }

            ciclo.avanzarSiCorresponde(instante);
            sala.actualizarBloqueActual(ciclo.getTipo());

            Duration duracion = Duration.between(
                    ciclo.getInicioBloque(),
                    ciclo.getFinBloque()
            );

            for (var participante : integrantes) {
                participante.getSesionPersonal()
                        .iniciarBloqueCompartido(
                                ciclo.getTipo(),
                                duracion,
                                ciclo.getInicioBloque()
                        );
            }
        }
    }

    // El participante nuevo todavía no debe estar persistido.
    public static void incorporarEnTransaccion(
            EntityManager em,
            SalaPrivada sala,
            ParticipacionSalaPrivada participante,
            Instant ahora
    ) {
        if (!sala.getCiclo().isIniciado()) {
            return;
        }

        Instant instante = normalizar(ahora);
        avanzarEnTransaccion(em, sala, instante);

        new PoliticaAdmisionSala().exigirAceptacion(
                sala.getBloqueActual(), participantes(em, sala).size(), true);

        crearSesion(em, sala, participante, instante);
    }

    private static void crearSesion(
            EntityManager em,
            SalaPrivada sala,
            ParticipacionSalaPrivada participante,
            Instant ahora
    ) {
        UUID id = participante.getEstudianteId();

        var estudiante = em.find(
                Estudiante.class,
                id,
                LockModeType.PESSIMISTIC_WRITE
        );

        if (estudiante == null) {
            throw new IllegalStateException(
                    "El estudiante no está disponible."
            );
        }

        long activas = em.createQuery(
                "select count(s) from SesionEstudio s "
                        + "where s.estudiante.id = :id "
                        + "and s.estado = :estado",
                Long.class
        ).setParameter(
                "id", id
        ).setParameter(
                "estado", EstadoSesion.ACTIVA
        ).getSingleResult();

        if (activas > 0) {
            throw new IllegalStateException(
                    "Un participante ya tiene una sesión activa."
            );
        }

        var configuracion = new ConfiguracionPomodoro(
                Duration.ofMinutes(25),
                Duration.ofMinutes(5),
                Duration.ofMinutes(15),
                4
        );

        var personal =
                new SesionEstudio(estudiante, configuracion, ahora);

        personal.iniciarBloqueCompartido(
                sala.getCiclo().getTipo(),
                sala.getCiclo().tiempoRestante(ahora),
                ahora
        );

        em.persist(personal);
        participante.asociarSesion(personal);
    }

    private static List<ParticipacionSalaPrivada> participantes(
            EntityManager em,
            SalaPrivada sala
    ) {
        return em.createQuery(
                "select p from ParticipacionSalaPrivada p "
                        + "where p.sala.id = :sala "
                        + "order by p.estudianteId",
                ParticipacionSalaPrivada.class
        ).setParameter(
                "sala", sala.getId()
        ).getResultList();
    }

    private SalaPrivada salaDe(
            EntityManager em,
            UUID estudianteId
    ) {
        var participante = em.find(
                ParticipacionSalaPrivada.class,
                Objects.requireNonNull(estudianteId)
        );

        if (participante == null) {
            throw new SecurityException(
                    "No participas en una sala privada."
            );
        }

        var sala = participante.getSala();
        em.refresh(sala, LockModeType.PESSIMISTIC_WRITE);

        if (sala.getEstado() != SalaPrivada.Estado.ABIERTA) {
            throw new IllegalStateException("La sala está cerrada.");
        }

        try {
            em.refresh(participante);
        } catch (EntityNotFoundException error) {
            throw new SecurityException(
                    "Tu participación terminó.",
                    error
            );
        }

        if (!participante.getSala().getId().equals(sala.getId())) {
            throw new SecurityException(
                    "Tu participación cambió."
            );
        }

        return sala;
    }

    private static Instant normalizar(Instant ahora) {
        return Objects.requireNonNull(ahora)
                .truncatedTo(ChronoUnit.MILLIS);
    }

    private <T> T transaccion(Function<EntityManager, T> accion) {
        try (var em = factory.createEntityManager()) {
            var tx = em.getTransaction();

            try {
                tx.begin();

                T resultado = accion.apply(em);

                tx.commit();
                return resultado;
            } catch (RuntimeException error) {
                if (tx.isActive()) {
                    tx.rollback();
                }
                throw error;
            }
        }
    }
}
