package epn.edu.ec.service;

import epn.edu.ec.model.*;
import epn.edu.ec.tdd.chat.ValidadorMensajes;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.*;

public class ServicioChatSalaPrivada {

    private final EntityManagerFactory factory;
    private final ValidadorMensajes validador =
            new ValidadorMensajes();

    public ServicioChatSalaPrivada(EntityManagerFactory factory) {
        this.factory = Objects.requireNonNull(factory);
    }

    public MensajeSalaPrivada registrar(
            UUID estudianteId,
            String texto,
            Instant ahora) {

        Objects.requireNonNull(estudianteId);
        Objects.requireNonNull(ahora);

        validador.validar(texto);

        try (var em = factory.createEntityManager()) {
            var tx = em.getTransaction();

            try {
                tx.begin();

                SalaPrivada sala = exigirSala(
                        em,
                        estudianteId,
                        LockModeType.PESSIMISTIC_WRITE
                );

                ServicioCicloSalaPrivada.avanzarEnTransaccion(
                        em,
                        sala,
                        ahora
                );

                if (sala.getBloqueActual()
                        == TipoBloque.CONCENTRACION) {

                    throw new IllegalStateException(
                            "El chat se habilitará en el próximo descanso."
                    );
                }

                Estudiante autor =
                        em.find(Estudiante.class, estudianteId);

                if (autor == null) {
                    throw new SecurityException(
                            "El estudiante no está disponible."
                    );
                }

                var mensaje = new MensajeSalaPrivada(
                        texto,
                        autor,
                        sala,
                        ahora
                );

                em.persist(mensaje);
                tx.commit();

                return mensaje;

            } catch (RuntimeException error) {
                if (tx.isActive()) {
                    tx.rollback();
                }

                throw error;
            }
        }
    }

    public List<MensajeSalaPrivada> historial(UUID estudianteId) {
        Objects.requireNonNull(estudianteId);

        try (var em = factory.createEntityManager()) {
            var tx = em.getTransaction();

            try {
                tx.begin();

                SalaPrivada sala = exigirSala(
                        em,
                        estudianteId,
                        LockModeType.PESSIMISTIC_READ
                );

                var mensajes = new ArrayList<>(
                        em.createQuery(
                                        "select m from MensajeSalaPrivada m "
                                                + "where m.sala.id = :sala "
                                                + "order by m.id desc",
                                        MensajeSalaPrivada.class
                                )
                                .setParameter("sala", sala.getId())
                                .setMaxResults(100)
                                .getResultList()
                );

                Collections.reverse(mensajes);
                tx.commit();

                return mensajes;

            } catch (RuntimeException error) {
                if (tx.isActive()) {
                    tx.rollback();
                }

                throw error;
            }
        }
    }

    private SalaPrivada exigirSala(
            EntityManager em,
            UUID estudianteId,
            LockModeType bloqueo) {

        var participacion = em.find(
                ParticipacionSalaPrivada.class,
                estudianteId
        );

        if (participacion == null) {
            throw new SecurityException(
                    "No participas en una sala privada."
            );
        }

        SalaPrivada sala = participacion.getSala();

        em.refresh(sala, bloqueo);

        if (sala.getEstado() != SalaPrivada.Estado.ABIERTA) {
            throw new IllegalStateException(
                    "La sala está cerrada."
            );
        }

        try {
            em.refresh(participacion);
        } catch (EntityNotFoundException error) {
            throw new SecurityException(
                    "Tu participación terminó.",
                    error
            );
        }

        if (!participacion.getSala().getId().equals(sala.getId())) {
            throw new SecurityException(
                    "Tu participación cambió de sala."
            );
        }

        return sala;
    }
}