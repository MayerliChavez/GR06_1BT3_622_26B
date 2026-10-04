package edu.proyecto.config;
import jakarta.persistence.*;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebListener;
import edu.proyecto.repository.RepositorioSesionesJpa;
import edu.proyecto.service.ServicioPomodoro;
@WebListener
public class PersistenceListener implements ServletContextListener {
    public void contextInitialized(ServletContextEvent event) {
        var factory = Persistence.createEntityManagerFactory("tarea3");
        event.getServletContext().setAttribute("emf", factory);
        var repositorio = new RepositorioSesionesJpa(factory);
        event.getServletContext().setAttribute("repositorioSesiones", repositorio);
        event.getServletContext().setAttribute("servicioPomodoro", new ServicioPomodoro(repositorio));
    }
    public void contextDestroyed(ServletContextEvent event) {
        var factory = (EntityManagerFactory) event.getServletContext().getAttribute("emf");
        if (factory != null && factory.isOpen()) factory.close();
    }
}
