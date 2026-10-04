package edu.proyecto.repository;

import edu.proyecto.model.*;
import jakarta.persistence.*;
import java.util.*;

/** Adaptador técnico de la interfaz del diagrama; no es una entidad. */
public class RepositorioSesionesJpa implements RepositorioSesiones {
    private final EntityManagerFactory factory;
    public RepositorioSesionesJpa(EntityManagerFactory factory) { this.factory = Objects.requireNonNull(factory); }

    @Override public void guardar(SesionEstudio sesion) {
        try (var em = factory.createEntityManager()) {
            var tx = em.getTransaction();
            try { tx.begin(); em.merge(sesion); tx.commit(); }
            catch (RuntimeException e) { if (tx.isActive()) tx.rollback(); throw e; }
        }
    }

    @Override public Optional<SesionEstudio> buscarPorId(UUID id) {
        try (var em = factory.createEntityManager()) {
            var sesion = em.find(SesionEstudio.class, id);
            if (sesion != null) sesion.getBloques().size(); // Cargar composición antes de cerrar JPA.
            return Optional.ofNullable(sesion);
        }
    }

    @Override public List<SesionEstudio> buscarPorEstudiante(UUID estudianteId) {
        try (var em = factory.createEntityManager()) {
            var sesiones = em.createQuery("select s from SesionEstudio s where s.estudiante.id = :id order by s.fechaInicio desc, s.id", SesionEstudio.class)
                    .setParameter("id", estudianteId).getResultList();
            sesiones.forEach(s -> s.getBloques().size());
            return sesiones;
        }
    }

    @Override public Optional<SesionEstudio> buscarActivaPorEstudiante(UUID estudianteId) {
        try (var em = factory.createEntityManager()) {
            var sesiones = em.createQuery("select s from SesionEstudio s where s.estudiante.id = :id and s.estado = :estado order by s.fechaInicio desc", SesionEstudio.class)
                    .setParameter("id", estudianteId).setParameter("estado", EstadoSesion.ACTIVA).setMaxResults(1).getResultList();
            sesiones.forEach(s -> s.getBloques().size());
            return sesiones.stream().findFirst();
        }
    }
}
