package epn.edu.ec.repository;
import epn.edu.ec.model.*;
import epn.edu.ec.tdd.preferencias.RepositorioPreferencias;
import jakarta.persistence.EntityManagerFactory;
import java.time.Duration;
import java.util.UUID;

public class RepositorioPreferenciasJpa implements RepositorioPreferencias {
    private final EntityManagerFactory factory;
    public RepositorioPreferenciasJpa(EntityManagerFactory factory) { this.factory = factory; }
    public ConfiguracionPomodoro buscarPorEstudiante(UUID estudianteId) {
        try (var em = factory.createEntityManager()) {
            var preferencias = em.find(PreferenciasEstudiante.class, estudianteId);
            return preferencias == null ? new ConfiguracionPomodoro(Duration.ofMinutes(25),
                    Duration.ofMinutes(5), Duration.ofMinutes(15), 4) : preferencias.getConfiguracion();
        }
    }
    public void guardar(UUID estudianteId, ConfiguracionPomodoro configuracion) {
        try (var em = factory.createEntityManager()) {
            var tx = em.getTransaction();
            try {
                tx.begin();
                em.merge(new PreferenciasEstudiante(estudianteId, configuracion));
                tx.commit();
            } catch (RuntimeException error) {
                if (tx.isActive()) tx.rollback();
                throw error;
            }
        }
    }
}
