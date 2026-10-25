package epn.edu.ec.web;

import epn.edu.ec.repository.RepositorioRachasJpa;
import epn.edu.ec.tdd.rachas.ControladorRacha;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.persistence.EntityManagerFactory;
import java.time.LocalDate;
import java.util.UUID;

final class VistaRacha {
    private VistaRacha() {}
    static void preparar(HttpServletRequest req, UUID estudianteId) {
        var factory = (EntityManagerFactory) req.getServletContext().getAttribute("emf");
        var racha = new RepositorioRachasJpa(factory).consultar(estudianteId);
        req.setAttribute("rachaDias", racha.diasVigentes(LocalDate.now(ControladorRacha.ZONA_ESTUDIO)));
    }
}
