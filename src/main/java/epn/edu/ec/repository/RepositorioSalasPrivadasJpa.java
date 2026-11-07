package epn.edu.ec.repository;

import epn.edu.ec.model.*;
import jakarta.persistence.EntityManagerFactory;
import java.util.*;

public class RepositorioSalasPrivadasJpa implements RepositorioSalasPrivadas {
    private final EntityManagerFactory factory;

    public RepositorioSalasPrivadasJpa(EntityManagerFactory factory) {
        this.factory = Objects.requireNonNull(factory);
    }

    @Override public Optional<SalaPrivada> buscarAbiertaPorEstudiante(UUID estudianteId) {
        try (var em = factory.createEntityManager()) {
            var participacion = em.find(ParticipacionSalaPrivada.class, estudianteId);
            if (participacion == null || participacion.getSala().getEstado() != SalaPrivada.Estado.ABIERTA)
                return Optional.empty();
            return Optional.of(participacion.getSala());
        }
    }

    @Override public Optional<SalaPrivada> buscarAbiertaPorCodigo(String codigo) {
        try (var em = factory.createEntityManager()) {
            return em.createQuery("select p.sala from ParticipacionSalaPrivada p "
                    + "where p.codigoReservado = :codigo and p.sala.estado = :estado", SalaPrivada.class)
                    .setParameter("codigo", codigo).setParameter("estado", SalaPrivada.Estado.ABIERTA)
                    .getResultStream().findFirst();
        }
    }

    @Override public void guardarNueva(SalaPrivada sala) {
        try (var em = factory.createEntityManager()) {
            var tx = em.getTransaction();
            try {
                tx.begin();
                em.persist(sala);
                em.persist(new ParticipacionSalaPrivada(sala.getResponsableId(), sala));
                tx.commit();
            } catch (RuntimeException error) {
                if (tx.isActive()) tx.rollback();
                if (buscarAbiertaPorEstudiante(sala.getResponsableId()).isPresent())
                    throw new IllegalStateException("Ya participas en una sala privada. Vuelve a tu sala actual.", error);
                if (buscarAbiertaPorCodigo(sala.getCodigo()).isPresent())
                    throw new CodigoSalaOcupadoException();
                throw error;
            }
        }
    }
}
