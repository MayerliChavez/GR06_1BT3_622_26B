package epn.edu.ec.repository;

import epn.edu.ec.model.*;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import epn.edu.ec.service.PoliticaAdmisionSala;
import java.util.*;
import java.util.function.Consumer;

public class RepositorioSolicitudesSalaJpa {

    private final EntityManagerFactory factory;

    public RepositorioSolicitudesSalaJpa(EntityManagerFactory factory) {
        this.factory = Objects.requireNonNull(factory);
    }

    public Optional<SolicitudIngresoSala> buscarPendientePorEstudiante(
            UUID estudianteId) {

        try (var em = factory.createEntityManager()) {
            return em.createQuery(
                            "select s from SolicitudIngresoSala s join fetch s.sala "
                                    + "where s.estudianteId = :estudiante "
                                    + "and s.estado = :estado",
                            SolicitudIngresoSala.class
                    )
                    .setParameter("estudiante", estudianteId)
                    .setParameter(
                            "estado",
                            SolicitudIngresoSala.Estado.PENDIENTE
                    )
                    .getResultStream()
                    .findFirst();
        }
    }

    public Optional<SolicitudIngresoSala> buscarPorId(UUID id) {
        try (var em = factory.createEntityManager()) {
            return Optional.ofNullable(
                    em.find(SolicitudIngresoSala.class, id)
            );
        }
    }

    public List<SolicitudIngresoSala> pendientesDeSala(UUID salaId) {
        try (var em = factory.createEntityManager()) {
            return em.createQuery(
                            "select s from SolicitudIngresoSala s join fetch s.sala "
                                    + "where s.sala.id = :sala and s.estado = :estado "
                                    + "order by s.fechaSolicitud, s.id",
                            SolicitudIngresoSala.class
                    )
                    .setParameter("sala", salaId)
                    .setParameter(
                            "estado",
                            SolicitudIngresoSala.Estado.PENDIENTE
                    )
                    .getResultList();
        }
    }

    public long contarParticipantes(UUID salaId) {
        try (var em = factory.createEntityManager()) {
            return em.createQuery(
                            "select count(p) from ParticipacionSalaPrivada p "
                                    + "where p.sala.id = :sala",
                            Long.class
                    )
                    .setParameter("sala", salaId)
                    .getSingleResult();
        }
    }

    public void guardar(SolicitudIngresoSala solicitud) {
        try (var em = factory.createEntityManager()) {
            var tx = em.getTransaction();

            try {
                tx.begin();
                em.merge(solicitud);
                tx.commit();
            } catch (RuntimeException error) {
                if (tx.isActive()) {
                    tx.rollback();
                }
                throw error;
            }
        }
    }

    public void decidir(UUID responsableId, UUID solicitudId, boolean aceptar) {
        enTransaccion(em -> {
            var solicitud = bloquearSolicitud(em, solicitudId);
            var sala = solicitud.getSala();
            if (!sala.getResponsableId().equals(responsableId))
                throw new SecurityException("Solo el responsable puede atender solicitudes.");
            if (sala.getEstado() != SalaPrivada.Estado.ABIERTA)
                throw new IllegalStateException("La sala está cerrada.");
            if (solicitud.getEstado() != SolicitudIngresoSala.Estado.PENDIENTE)
                throw new IllegalStateException("La solicitud ya fue atendida.");

            if (!aceptar) {
                solicitud.rechazar(responsableId);
                return;
            }

            long ocupacion = em.createQuery(
                    "select count(p) from ParticipacionSalaPrivada p where p.sala.id = :sala", Long.class)
                    .setParameter("sala", sala.getId()).getSingleResult();
            if (ocupacion >= 4)
                throw new IllegalStateException("La sala está llena. No es posible aceptar más participantes.");
            if (!new PoliticaAdmisionSala().puedeAceptar(sala.getBloqueActual(), (int) ocupacion, true))
                throw new IllegalStateException("Podrás aceptar nuevos participantes en el siguiente descanso.");

            UUID estudianteId = solicitud.getEstudianteId();
            // También serializa aceptaciones del mismo estudiante en salas distintas.
            if (em.find(Estudiante.class, estudianteId, LockModeType.PESSIMISTIC_WRITE) == null)
                throw new IllegalStateException("El estudiante de la solicitud ya no está disponible.");
            if (em.find(ParticipacionSalaPrivada.class, estudianteId) != null)
                throw new IllegalStateException("El estudiante ya participa en otra sala privada.");
            long individuales = em.createQuery(
                    "select count(s) from SesionEstudio s where s.estudiante.id = :id and s.estado = :estado", Long.class)
                    .setParameter("id", estudianteId).setParameter("estado", EstadoSesion.ACTIVA).getSingleResult();
            long emparejadas = em.createQuery(
                    "select count(s) from SesionCompartida s join s.participaciones p "
                            + "where p.estudiante.id = :id and p.fechaFin is null and s.estado = :estado", Long.class)
                    .setParameter("id", estudianteId).setParameter("estado", EstadoSesionCompartida.ACTIVA).getSingleResult();
            if (individuales > 0 || emparejadas > 0)
                throw new IllegalStateException("El estudiante ya tiene una sesión de estudio en curso.");

            solicitud.aceptar(responsableId);
            em.persist(new ParticipacionSalaPrivada(estudianteId, sala));
        });
    }

    public void cancelarPendiente(UUID estudianteId) {
        enTransaccion(em -> {
            var pendientes = em.createQuery(
                    "select s.id from SolicitudIngresoSala s where s.estudianteId = :id and s.estado = :estado", UUID.class)
                    .setParameter("id", estudianteId).setParameter("estado", SolicitudIngresoSala.Estado.PENDIENTE)
                    .getResultList();
            if (pendientes.isEmpty()) throw new IllegalStateException("No tienes una solicitud pendiente.");
            bloquearSolicitud(em, pendientes.get(0)).cancelar(estudianteId);
        });
    }

    public Optional<SolicitudIngresoSala> buscarUltimaPorEstudiante(UUID estudianteId) {
        try (var em = factory.createEntityManager()) {
            return em.createQuery("select s from SolicitudIngresoSala s join fetch s.sala "
                    + "where s.estudianteId = :id order by s.fechaSolicitud desc, s.id desc", SolicitudIngresoSala.class)
                    .setParameter("id", estudianteId).setMaxResults(1).getResultStream().findFirst();
        }
    }

    private SolicitudIngresoSala bloquearSolicitud(EntityManager em, UUID solicitudId) {
        var solicitud = em.find(SolicitudIngresoSala.class, solicitudId);
        if (solicitud == null) throw new IllegalArgumentException("La solicitud no existe.");
        // Orden común: sala, solicitud y (al aceptar) estudiante. Se relee tras obtener los bloqueos.
        em.refresh(solicitud.getSala(), LockModeType.PESSIMISTIC_WRITE);
        em.refresh(solicitud, LockModeType.PESSIMISTIC_WRITE);
        return solicitud;
    }

    private void enTransaccion(Consumer<EntityManager> accion) {
        try (var em = factory.createEntityManager()) {
            var tx = em.getTransaction();
            try {
                tx.begin();
                accion.accept(em);
                tx.commit();
            } catch (RuntimeException error) {
                if (tx.isActive()) tx.rollback();
                throw error;
            }
        }
    }
}
