package epn.edu.ec.web;

import epn.edu.ec.model.Estudiante;
import epn.edu.ec.service.ServicioIngresoSala;
import epn.edu.ec.service.ServicioSalasPrivadas;

import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Objects;
import java.util.UUID;

@WebServlet("/salas")
public class SalasPrivadasServlet extends HttpServlet {

    private ServicioSalasPrivadas salas() {
        return (ServicioSalasPrivadas) getServletContext()
                .getAttribute("servicioSalasPrivadas");
    }

    private ServicioIngresoSala ingreso() {
        return (ServicioIngresoSala) getServletContext()
                .getAttribute("servicioIngresoSala");
    }

    @Override
    protected void doGet(
            HttpServletRequest req,
            HttpServletResponse resp
    ) throws ServletException, IOException {

        resp.setHeader("Cache-Control", "no-store");
        resp.setContentType("text/html;charset=UTF-8");

        UUID estudianteId = IdentidadWeb.estudiante(req, resp);

        var sala = salas()
                .consultarActual(estudianteId)
                .orElse(null);

        boolean esResponsable =
                sala != null
                        && sala.getResponsableId().equals(estudianteId);

        req.setAttribute("csrf", IdentidadWeb.csrf(req));
        req.setAttribute("sala", sala);
        req.setAttribute("esResponsable", esResponsable);

        req.setAttribute(
                "pendiente",
                ingreso().consultarPendiente(estudianteId).orElse(null)
        );

        if (esResponsable) {
            var solicitudes =
                    ingreso().pendientesParaResponsable(estudianteId);

            var nombresSolicitantes = new HashMap<UUID, String>();

            var factory =
                    (EntityManagerFactory) getServletContext()
                            .getAttribute("emf");

            try (var em = factory.createEntityManager()) {
                for (var solicitud : solicitudes) {
                    var estudiante = em.find(
                            Estudiante.class,
                            solicitud.getEstudianteId()
                    );

                    String nombre = estudiante == null
                            ? "Estudiante"
                            : estudiante.getNombreVisible();

                    nombresSolicitantes.put(
                            solicitud.getEstudianteId(),
                            nombre
                    );
                }

                Long ocupacion = em.createQuery(
                                "select count(p) "
                                        + "from ParticipacionSalaPrivada p "
                                        + "where p.sala.id = :sala",
                                Long.class
                        )
                        .setParameter("sala", sala.getId())
                        .getSingleResult();

                req.setAttribute("ocupacion", ocupacion);
            }

            req.setAttribute("solicitudes", solicitudes);
            req.setAttribute(
                    "nombresSolicitantes",
                    nombresSolicitantes
            );
        }

        if ("espera".equals(req.getParameter("formato"))) {
            if (!esResponsable) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }

            req.getRequestDispatcher(
                    "/WEB-INF/views/salas-espera.jsp"
            ).forward(req, resp);

            return;
        }

        req.setAttribute(
                "mensaje",
                req.getSession().getAttribute("mensajeSala")
        );

        req.getSession().removeAttribute("mensajeSala");

        req.getRequestDispatcher(
                "/WEB-INF/views/salas.jsp"
        ).forward(req, resp);
    }

    @Override
    protected void doPost(
            HttpServletRequest req,
            HttpServletResponse resp
    ) throws IOException {

        req.setCharacterEncoding("UTF-8");
        resp.setHeader("Cache-Control", "no-store");

        UUID estudianteId = IdentidadWeb.estudiante(req, resp);

        String csrfEsperado = IdentidadWeb.csrf(req);
        String csrfRecibido = req.getParameter("csrf");

        if (!csrfEsperado.equals(csrfRecibido)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        String accion = Objects.toString(
                req.getParameter("accion"),
                ""
        );

        try {
            switch (accion) {
                case "crear" -> {
                    boolean tieneSolicitudPendiente =
                            ingreso().consultarPendiente(estudianteId)
                                    .isPresent();

                    if (tieneSolicitudPendiente) {
                        throw new IllegalStateException(
                                "Cancela tu solicitud pendiente "
                                        + "antes de crear una sala."
                        );
                    }

                    salas().crear(estudianteId);
                }

                case "solicitar" -> {
                    boolean solicitudRepetida =
                            ingreso().consultarPendiente(estudianteId)
                                    .isPresent();

                    ingreso().solicitar(
                            estudianteId,
                            req.getParameter("codigo"),
                            Instant.now()
                    );

                    if (solicitudRepetida) {
                        req.getSession().setAttribute(
                                "mensajeSala",
                                "Tu solicitud ya está esperando "
                                        + "la aprobación del responsable."
                        );
                    }
                }

                case "cancelarSolicitud" -> {
                    ingreso().cancelar(estudianteId);
                }

                default -> {
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                    return;
                }
            }
        } catch (IllegalArgumentException | IllegalStateException error) {
            req.getSession().setAttribute(
                    "mensajeSala",
                    error.getMessage()
            );
        }

        resp.sendRedirect(req.getContextPath() + "/salas");
    }
}