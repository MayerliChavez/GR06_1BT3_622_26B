package epn.edu.ec.service;

import epn.edu.ec.model.*;
import epn.edu.ec.repository.RepositorioRachasJpa;
import epn.edu.ec.tdd.rachas.ControladorRacha;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;

public class ServicioCierreSalaPrivada {

    private final EntityManagerFactory factory;
    private final ServicioAvisosCierreSala avisos;

    public ServicioCierreSalaPrivada(EntityManagerFactory factory) {
        this.factory = Objects.requireNonNull(factory);
        this.avisos = new ServicioAvisosCierreSala(factory);
    }

    public boolean cerrar(
            UUID responsableId,
            UUID salaId,
            boolean confirmado,
            Instant ahora
    ) {
        Objects.requireNonNull(responsableId);
        Objects.requireNonNull(salaId);

        Instant instante = Objects.requireNonNull(ahora)
                .truncatedTo(ChronoUnit.MILLIS);

        return enTransaccion(em -> {
            var sala = em.find(
                    SalaPrivada.class,
                    salaId,
                    LockModeType.PESSIMISTIC_WRITE
            );

            if (sala == null) {
                throw new IllegalArgumentException(
                        "La sala no existe."
                );
            }

            if (!sala.getResponsableId().equals(responsableId)) {
                throw new SecurityException(
                        "Solo el responsable puede cerrar la sala."
                );
            }

            if (!confirmado
                    || sala.getEstado() == SalaPrivada.Estado.CERRADA) {
                return false;
            }

            var integrantes = em.createQuery(
                    "select p from ParticipacionSalaPrivada p "
                            + "where p.sala.id = :sala "
                            + "order by p.estudianteId",
                    ParticipacionSalaPrivada.class
            ).setParameter(
                    "sala", salaId
            ).getResultList();

            for (var integrante : integrantes) {
                var estudiante = em.find(
                        Estudiante.class,
                        integrante.getEstudianteId(),
                        LockModeType.PESSIMISTIC_WRITE
                );

                if (estudiante == null) {
                    throw new IllegalStateException(
                            "Un participante ya no está disponible."
                    );
                }
            }

            ServicioCicloSalaPrivada.avanzarEnTransaccion(
                    em,
                    sala,
                    instante
            );

            for (var integrante : integrantes) {
                var personal = integrante.getSesionPersonal();

                if (sala.getCiclo().isIniciado() && personal == null) {
                    throw new IllegalStateException(
                            "Falta la sesión personal de un participante."
                    );
                }

                if (personal != null
                        && personal.getEstado() == EstadoSesion.ACTIVA) {
                    personal.finalizar(instante);

                    if (new ControladorRacha()
                            .esSesionCompletada(personal)) {
                        RepositorioRachasJpa.actualizar(
                                em,
                                integrante.getEstudianteId()
                        );
                    }
                }
            }

            sala.cerrar(responsableId, instante);

            for (var integrante : integrantes) {
                avisos.registrarEnTransaccion(
                        em,
                        integrante.getEstudianteId(),
                        responsableId,
                        salaId,
                        instante
                );

                // La sesión personal se conserva en el historial.
                em.remove(integrante);
            }

            return true;
        });
    }

    public Optional<String> consumirAviso(UUID estudianteId) {
        return avisos.consumirAviso(estudianteId);
    }

    private <T> T enTransaccion(Function<EntityManager, T> accion) {
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
