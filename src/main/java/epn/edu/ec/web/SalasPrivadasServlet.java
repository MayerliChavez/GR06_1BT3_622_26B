package epn.edu.ec.web;

import epn.edu.ec.model.Estudiante;
import epn.edu.ec.model.SolicitudIngresoSala;
import epn.edu.ec.service.PoliticaAdmisionSala;
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

        boolean cicloIniciado = false;

        if (sala != null) {
            cicloIniciado = ciclo().consultar(
                    estudianteId,
                    Instant.now()
            ).iniciado();

            // Recupera también el bloque actualizado para la admisión.
            sala = salas()
                    .consultarActual(estudianteId)
                    .orElse(null);
        }

        req.setAttribute("cicloIniciado", cicloIniciado);

        boolean esResponsable =
                sala != null
                        && sala.getResponsableId().equals(estudianteId);

        var pendiente = ingreso().consultarPendiente(estudianteId).orElse(null);
        var ultima = ingreso().consultarUltima(estudianteId).orElse(null);
        if ("estadoSolicitud".equals(req.getParameter("formato"))) {
            String estado = sala != null ? "ACEPTADA" : pendiente != null ? "PENDIENTE"
                    : ultima != null && ultima.getEstado() == SolicitudIngresoSala.Estado.RECHAZADA
                            ? "RECHAZADA" : "SIN_SOLICITUD";
            resp.setContentType("application/json;charset=UTF-8");
            resp.getWriter().write("{\"estado\":\"" + estado + "\"}");
            return;
        }

        req.setAttribute("csrf", IdentidadWeb.csrf(req));
        req.setAttribute("sala", sala);
        req.setAttribute("esResponsable", esResponsable);
        req.setAttribute("maxParticipantes", PoliticaAdmisionSala.MAX_PARTICIPANTES);

        req.setAttribute(
                "pendiente",
                pendiente
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
                var decision = new PoliticaAdmisionSala().evaluar(
                        sala.getBloqueActual(), Math.toIntExact(ocupacion), esResponsable);
                req.setAttribute("aceptacionPermitida", decision.permitida());
                req.setAttribute("motivoAdmision", decision.mensaje());
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
        if (req.getAttribute("mensaje") == null && sala == null && pendiente == null && ultima != null
                && ultima.getEstado() == SolicitudIngresoSala.Estado.RECHAZADA) {
            req.setAttribute("mensaje", "Tu solicitud de ingreso no fue aceptada.");
        }

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

                case "aceptarSolicitud" -> {
                    ingreso().aceptar(estudianteId, UUID.fromString(req.getParameter("solicitudId")));
                    req.getSession().setAttribute("mensajeSala", "Solicitud aceptada.");
                }

                case "rechazarSolicitud" -> {
                    ingreso().rechazar(estudianteId, UUID.fromString(req.getParameter("solicitudId")));
                    req.getSession().setAttribute("mensajeSala", "Solicitud rechazada.");
                }

                case "iniciarEstudio" -> {
                    ciclo().iniciar(
                            estudianteId,
                            Instant.now()
                    );

                    req.getSession().setAttribute(
                            "mensajeSala",
                            "El estudio compartido comenzó."
                    );
                }

                case "cerrarSala" -> {
                    var cierre =
                            (epn.edu.ec.service.ServicioCierreSalaPrivada)
                                    getServletContext().getAttribute(
                                            "servicioCierreSalaPrivada"
                                    );

                    boolean cerrado = cierre.cerrar(
                            estudianteId,
                            UUID.fromString(req.getParameter("salaId")),
                            "true".equals(req.getParameter("confirmado")),
                            Instant.now()
                    );

                    if (cerrado) {
                        resp.sendRedirect(
                                req.getContextPath() + "/pomodoro"
                        );
                        return;
                    }
                }

                default -> {
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                    return;
                }
            }
        } catch (SecurityException error) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        } catch (IllegalArgumentException | IllegalStateException error) {
            req.getSession().setAttribute(
                    "mensajeSala",
                    error.getMessage()
            );
        }

        resp.sendRedirect(req.getContextPath() + "/salas");
    }

    private epn.edu.ec.service.ServicioCicloSalaPrivada ciclo() {
        return (epn.edu.ec.service.ServicioCicloSalaPrivada)
                getServletContext().getAttribute(
                        "servicioCicloSalaPrivada"
                );
    }
}
