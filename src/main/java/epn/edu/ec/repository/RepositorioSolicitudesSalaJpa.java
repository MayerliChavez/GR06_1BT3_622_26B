package epn.edu.ec.repository;

import epn.edu.ec.model.*;
import jakarta.persistence.EntityManagerFactory;
import java.util.*;

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
}