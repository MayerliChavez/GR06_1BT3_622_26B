package epn.edu.ec.web;

import epn.edu.ec.repository.RepositorioRachasJpa;
import epn.edu.ec.tdd.rachas.ControladorRacha;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.time.LocalDate;
import java.util.stream.Collectors;

@WebServlet("/racha")
public class RachaServlet extends HttpServlet {
    @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        var id = IdentidadWeb.estudiante(req, resp);
        var factory = (EntityManagerFactory) getServletContext().getAttribute("emf");
        var resumen = new RepositorioRachasJpa(factory).resumen(id);
        var hoy = LocalDate.now(ControladorRacha.ZONA_ESTUDIO);
        // Solo fechas ISO del servidor y numeros; nunca datos de texto del usuario.
        var fechas = resumen.diasEstudiados().stream().map(d -> "\"" + d + "\"").collect(Collectors.joining(","));
        resp.setHeader("Cache-Control", "no-store");
        resp.setContentType("application/json;charset=UTF-8");
        resp.getWriter().printf(java.util.Locale.ROOT, "{\"rachaDias\":%d,\"hoy\":\"%s\",\"fechas\":[%s]}",
                resumen.racha().diasVigentes(hoy), hoy, fechas);
    }
}
