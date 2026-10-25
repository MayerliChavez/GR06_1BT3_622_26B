package epn.edu.ec.repository;
import epn.edu.ec.model.*;
import jakarta.persistence.*;
import java.util.*;

public class RepositorioSesionesCompartidasJpa implements RepositorioSesionesCompartidas {
    private final EntityManagerFactory factory;
    public RepositorioSesionesCompartidasJpa(EntityManagerFactory factory) { this.factory = Objects.requireNonNull(factory); }
    @Override public void guardar(SesionCompartida sesion) {
        try (var em = factory.createEntityManager()) {
            var tx = em.getTransaction();
            try {
                tx.begin();
                for (var individual : sesion.getSesiones()) {
                    var cuenta = em.find(CuentaUsuario.class, individual.getEstudiante().getId());
                    if (cuenta != null) individual.getEstudiante().cambiarNombreVisible(cuenta.getEstudiante().getNombreVisible());
                }
                em.merge(sesion);
                for (var individual : sesion.getSesiones().stream()
                        .sorted(Comparator.comparing(s -> s.getEstudiante().getId())).toList()) {
                    if (new epn.edu.ec.tdd.rachas.ControladorRacha().esSesionCompletada(individual))
                        RepositorioRachasJpa.actualizar(em, individual.getEstudiante().getId());
                }
                tx.commit();
            }
            catch (RuntimeException e) { if (tx.isActive()) tx.rollback(); throw e; }
        }
    }
    @Override public Optional<SesionCompartida> buscarPorId(UUID id) {
        try (var em = factory.createEntityManager()) {
            var sesion = em.find(SesionCompartida.class, id);
            if (sesion != null) cargarComposicion(sesion);
            return Optional.ofNullable(sesion);
        }
    }
    @Override public Optional<SesionCompartida> buscarAbiertaPorEstudiante(UUID estudianteId) {
        try (var em = factory.createEntityManager()) {
            var sesiones = em.createQuery("select distinct s from SesionCompartida s join s.participaciones p where s.estado = :estado and p.estudiante.id = :id and p.fechaFin is null", SesionCompartida.class)
                    .setParameter("estado", EstadoSesionCompartida.ACTIVA).setParameter("id", estudianteId).getResultList();
            sesiones.forEach(this::cargarComposicion);
            return sesiones.stream().findFirst();
        }
    }
    private void cargarComposicion(SesionCompartida sesion) {
        sesion.getSesiones().forEach(s -> s.getBloques().size());
        sesion.getParticipaciones().forEach(p -> p.getSesionEstudio().getBloques().size());
    }
}
