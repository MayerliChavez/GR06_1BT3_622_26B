package edu.proyecto.config;
import jakarta.persistence.*;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebListener;
import edu.proyecto.repository.RepositorioSesionesJpa;
import edu.proyecto.service.ServicioPomodoro;
import edu.proyecto.service.ServicioEstudioCompartido;
import edu.proyecto.repository.*;
import edu.proyecto.model.ConfiguracionPomodoro;
import edu.proyecto.web.PresenciaCompartida;
import java.time.*;
import java.util.concurrent.*;
@WebListener
public class PersistenceListener implements ServletContextListener {
    public void contextInitialized(ServletContextEvent event) {
        var factory = Persistence.createEntityManagerFactory("tarea3");
        event.getServletContext().setAttribute("emf", factory);
        event.getServletContext().setAttribute("servicioCuentas", new edu.proyecto.service.ServicioCuentas(factory));
        var repositorio = new RepositorioSesionesJpa(factory);
        event.getServletContext().setAttribute("repositorioSesiones", repositorio);
        event.getServletContext().setAttribute("servicioPomodoro", new ServicioPomodoro(repositorio));
        var compartidas = new RepositorioSesionesCompartidasJpa(factory);
        var compartido = new ServicioEstudioCompartido(new EmparejadorEnMemoria(), repositorio, compartidas,
                new ConfiguracionPomodoro(Duration.ofMinutes(25), Duration.ofMinutes(5), Duration.ofMinutes(15), 4));
        var presencia = new PresenciaCompartida(compartido, Duration.ofSeconds(90));
        // Tras un reinicio, las participaciones abiertas disponen de una nueva ventana de reconexión.
        try (var em = factory.createEntityManager()) {
            var abiertas = em.createQuery("select s from SesionCompartida s where s.estado = :estado", edu.proyecto.model.SesionCompartida.class)
                    .setParameter("estado", edu.proyecto.model.EstadoSesionCompartida.ACTIVA).getResultList();
            var ahora = Instant.now();
            for (var s : abiertas) for (var p : s.getParticipaciones()) {
                if (p.estaPresente()) presencia.registrar(p.getEstudiante().getId(), s.getId(), ahora);
            }
        }
        event.getServletContext().setAttribute("repositorioCompartidas", compartidas);
        event.getServletContext().setAttribute("servicioCompartido", compartido);
        event.getServletContext().setAttribute("presenciaCompartida", presencia);
        var monitor = Executors.newSingleThreadScheduledExecutor(r -> {
            var hilo = new Thread(r, "pomora-presencia"); hilo.setDaemon(true); return hilo;
        });
        monitor.scheduleWithFixedDelay(() -> {
            try { presencia.revisar(Instant.now()); }
            catch (RuntimeException error) { event.getServletContext().log("No se pudo verificar la presencia compartida.", error); }
        }, 10, 10, TimeUnit.SECONDS);
        event.getServletContext().setAttribute("monitorPresencia", monitor);
    }
    public void contextDestroyed(ServletContextEvent event) {
        var monitor = (ScheduledExecutorService) event.getServletContext().getAttribute("monitorPresencia");
        if (monitor != null) {
            monitor.shutdownNow();
            try { monitor.awaitTermination(10, TimeUnit.SECONDS); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
        var factory = (EntityManagerFactory) event.getServletContext().getAttribute("emf");
        if (factory != null && factory.isOpen()) factory.close();
    }
}
