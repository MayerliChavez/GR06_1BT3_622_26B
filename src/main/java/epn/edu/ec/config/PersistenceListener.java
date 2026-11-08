package epn.edu.ec.config;
import jakarta.persistence.*;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebListener;
import epn.edu.ec.repository.RepositorioSesionesJpa;
import epn.edu.ec.service.ServicioPomodoro;
import epn.edu.ec.service.ServicioEstudioCompartido;
import epn.edu.ec.repository.*;
import epn.edu.ec.model.ConfiguracionPomodoro;
import epn.edu.ec.web.PresenciaCompartida;
import java.time.*;
import java.util.concurrent.*;
@WebListener
public class PersistenceListener implements ServletContextListener {
    public void contextInitialized(ServletContextEvent event) {
        var jdbc = System.getProperty("pomora.jdbc.url");
        var factory = jdbc == null ? Persistence.createEntityManagerFactory("tarea3")
                : Persistence.createEntityManagerFactory("tarea3", java.util.Map.of("jakarta.persistence.jdbc.url", jdbc));
        event.getServletContext().setAttribute("emf", factory);
        event.getServletContext().setAttribute("servicioCierreSalaPrivada", new epn.edu.ec.service.ServicioCierreSalaPrivada(factory));
        event.getServletContext().setAttribute("servicioCuentas", new epn.edu.ec.service.ServicioCuentas(factory));
        var repositorio = new RepositorioSesionesJpa(factory);
        var salas = new RepositorioSalasPrivadasJpa(factory);
        event.getServletContext().setAttribute("repositorioSalasPrivadas", salas);
        event.getServletContext().setAttribute("repositorioSesiones", repositorio);
        event.getServletContext().setAttribute("servicioPomodoro", new ServicioPomodoro(repositorio, salas));
        event.getServletContext().setAttribute("servicioPreferencias",
                new epn.edu.ec.tdd.preferencias.ServicioPreferencias(new RepositorioPreferenciasJpa(factory),
                        (ServicioPomodoro) event.getServletContext().getAttribute("servicioPomodoro")));
        var compartidas = new RepositorioSesionesCompartidasJpa(factory);
        var compartido = new ServicioEstudioCompartido(new EmparejadorEnMemoria(), repositorio, compartidas,
                new ConfiguracionPomodoro(Duration.ofMinutes(25), Duration.ofMinutes(5), Duration.ofMinutes(15), 4), salas);
        event.getServletContext().setAttribute("servicioSalasPrivadas",
                new epn.edu.ec.service.ServicioSalasPrivadas(salas, repositorio, compartidas,
                        new epn.edu.ec.service.GeneradorCodigoSalaAleatorio()));
        event.getServletContext().setAttribute(
                "servicioIngresoSala",
                new epn.edu.ec.service.ServicioIngresoSala(
                        new RepositorioSolicitudesSalaJpa(factory),
                        salas,
                        repositorio,
                        compartidas
                )
        );
        event.getServletContext().setAttribute("servicioChat", new epn.edu.ec.service.ServicioChat(factory, compartidas));
        event.getServletContext().setAttribute("servicioChatSalaPrivada",new epn.edu.ec.service.ServicioChatSalaPrivada(factory));
        event.getServletContext().setAttribute("conexionesChat", new java.util.concurrent.ConcurrentHashMap<String, epn.edu.ec.web.ChatSocket>());
        var presencia = new PresenciaCompartida(compartido, Duration.ofSeconds(90));
        // Tras un reinicio, las participaciones abiertas disponen de una nueva ventana de reconexión.
        try (var em = factory.createEntityManager()) {
            var abiertas = em.createQuery("select s from SesionCompartida s where s.estado = :estado", epn.edu.ec.model.SesionCompartida.class)
                    .setParameter("estado", epn.edu.ec.model.EstadoSesionCompartida.ACTIVA).getResultList();
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
        var cicloPrivado =
                new epn.edu.ec.service.ServicioCicloSalaPrivada(factory);

        event.getServletContext().setAttribute(
                "servicioCicloSalaPrivada",
                cicloPrivado
        );

        var monitorCiclo =
                Executors.newSingleThreadScheduledExecutor(r -> {
                    var hilo = new Thread(r, "pomora-ciclo-privado");
                    hilo.setDaemon(true);
                    return hilo;
                });

        monitorCiclo.scheduleWithFixedDelay(() -> {
            try {
                cicloPrivado.avanzarAbiertas(Instant.now());
                epn.edu.ec.web.CicloSalaSocket.publicarEstados();
            } catch (RuntimeException error) {
                event.getServletContext().log(
                        "No se pudo revisar el ciclo privado.",
                        error
                );
            }
        }, 1, 1, TimeUnit.SECONDS);

        event.getServletContext().setAttribute(
                "monitorCicloSalaPrivada",
                monitorCiclo
        );
    }
    public void contextDestroyed(ServletContextEvent event) {
        var monitorCiclo =
                (ScheduledExecutorService) event.getServletContext()
                        .getAttribute("monitorCicloSalaPrivada");

        if (monitorCiclo != null) {
            monitorCiclo.shutdownNow();

            try {
                monitorCiclo.awaitTermination(10, TimeUnit.SECONDS);
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
            }
        }
        var monitor = (ScheduledExecutorService) event.getServletContext().getAttribute("monitorPresencia");
        if (monitor != null) {
            monitor.shutdownNow();
            try { monitor.awaitTermination(10, TimeUnit.SECONDS); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }

        epn.edu.ec.web.CicloSalaSocket.cerrarTodas();

        var factory = (EntityManagerFactory) event.getServletContext().getAttribute("emf");
        if (factory != null && factory.isOpen()) factory.close();
    }
}
