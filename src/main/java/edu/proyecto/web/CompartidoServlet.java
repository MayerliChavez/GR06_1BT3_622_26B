package edu.proyecto.web;
import edu.proyecto.model.*;
import edu.proyecto.repository.*;
import edu.proyecto.service.ServicioEstudioCompartido;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.time.Instant;
import java.util.*;

@WebServlet("/compartido")
public class CompartidoServlet extends HttpServlet {
    @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setHeader("Cache-Control", "no-store");
        UUID id = IdentidadWeb.estudiante(req, resp);
        req.setAttribute("csrf", IdentidadWeb.csrf(req));
        prepararVista(req, id);
        if ("json".equals(req.getParameter("formato"))) { escribirEstado(req, resp); return; }
        req.setAttribute("mensaje", req.getSession().getAttribute("mensajeCompartido"));
        req.getSession().removeAttribute("mensajeCompartido");
        req.getRequestDispatcher("/WEB-INF/views/compartido.jsp").forward(req, resp);
    }
    @Override protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        req.setCharacterEncoding("UTF-8"); resp.setHeader("Cache-Control", "no-store");
        UUID id = IdentidadWeb.estudiante(req, resp);
        if (!IdentidadWeb.csrf(req).equals(req.getParameter("csrf"))) { resp.sendError(403); return; }
        var servicio = (ServicioEstudioCompartido) getServletContext().getAttribute("servicioCompartido");
        var repo = (RepositorioSesionesCompartidas) getServletContext().getAttribute("repositorioCompartidas");
        var presencia = (PresenciaCompartida) getServletContext().getAttribute("presenciaCompartida");
        try {
            synchronized (servicio) {
                Instant ahora = Instant.now();
                presencia.revisar(ahora);
                var abierta = repo.buscarAbiertaPorEstudiante(id);
                switch (Objects.toString(req.getParameter("accion"), "")) {
                    case "buscar" -> {
                        var resultado = servicio.solicitarEmparejamiento(id, ahora);
                        req.getSession().setAttribute("esperando", resultado.isEmpty());
                        if (resultado.isPresent()) {
                            for (var p : resultado.get().getParticipaciones()) presencia.registrar(p.getEstudiante().getId(), resultado.get().getId(), ahora);
                        } else presencia.registrar(id, null, ahora);
                    }
                    case "cancelar" -> {
                        servicio.cancelarEspera(id); presencia.olvidar(id);
                        req.getSession().setAttribute("esperando", false);
                    }
                    case "actualizar" -> {
                        if (abierta.isPresent()) {
                            presencia.registrar(id, abierta.get().getId(), ahora);
                            servicio.actualizarSesion(abierta.get().getId(), ahora);
                            req.getSession().setAttribute("esperando", false);
                        } else if (Boolean.TRUE.equals(req.getSession().getAttribute("esperando"))) {
                            // Revalidar la cola permite recuperarse de una espera vencida o de un reinicio.
                            var resultado = servicio.solicitarEmparejamiento(id, ahora);
                            if (resultado.isPresent()) {
                                for (var p : resultado.get().getParticipaciones()) presencia.registrar(p.getEstudiante().getId(), resultado.get().getId(), ahora);
                                req.getSession().setAttribute("esperando", false);
                            } else presencia.registrar(id, null, ahora);
                        }
                    }
                    case "salir", "desconectar" -> {
                        if (abierta.isPresent()) {
                            if ("salir".equals(req.getParameter("accion"))) servicio.salir(abierta.get().getId(), id, ahora);
                            else servicio.registrarDesconexion(abierta.get().getId(), id, ahora);
                        } else servicio.cancelarEspera(id);
                        presencia.olvidar(id); req.getSession().setAttribute("esperando", false);
                    }
                    default -> throw new IllegalArgumentException("Acción desconocida.");
                }
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
            if ("json".equals(req.getParameter("formato"))) resp.setStatus(409);
            else req.getSession().setAttribute("mensajeCompartido", e.getMessage());
        }
        prepararVista(req, id);
        if ("json".equals(req.getParameter("formato"))) escribirEstado(req, resp);
        else resp.sendRedirect(req.getContextPath() + "/compartido");
    }
    private void prepararVista(HttpServletRequest req, UUID id) {
        var servicio = (ServicioEstudioCompartido) getServletContext().getAttribute("servicioCompartido");
        var repo = (RepositorioSesionesCompartidas) getServletContext().getAttribute("repositorioCompartidas");
        var individuales = (RepositorioSesiones) getServletContext().getAttribute("repositorioSesiones");
        synchronized (servicio) {
            var abierta = repo.buscarAbiertaPorEstudiante(id);
            var s = abierta.orElse(null);
            var actual = s == null ? null : s.getParticipaciones().stream().filter(p -> p.getEstudiante().getId().equals(id)).findFirst().orElseThrow();
            var companero = s == null ? null : s.getParticipaciones().stream().filter(p -> !p.getEstudiante().getId().equals(id)).findFirst().orElseThrow();
            var bloques = actual == null ? List.<BloquePomodoro>of() : actual.getSesionEstudio().getBloques();
            var b = bloques.isEmpty() ? null : bloques.get(bloques.size() - 1);
            long restante = s == null ? 1500000 : servicio.consultarTiempoRestante(s.getId(), Instant.now()).orElse(java.time.Duration.ZERO).toMillis();
            req.setAttribute("compartidaId", s == null ? "" : s.getId().toString());
            req.setAttribute("esperando", s == null && Boolean.TRUE.equals(req.getSession().getAttribute("esperando")));
            req.setAttribute("hayCompartida", s != null);
            req.setAttribute("bloqueado", s == null && individuales.buscarActivaPorEstudiante(id).isPresent());
            req.setAttribute("restanteMillis", restante);
            req.setAttribute("objetivoMillis", b == null ? 1500000 : b.getDuracionObjetivo().toMillis());
            req.setAttribute("tipoBloque", b == null ? "CONCENTRACION" : b.getTipo().name());
            req.setAttribute("ordenBloque", b == null ? 1 : b.getOrden());
            req.setAttribute("companeroPresente", companero != null && companero.estaPresente());
            req.setAttribute("companeroNombre", companero == null ? "" : companero.getEstudiante().getNombreVisible());
            req.setAttribute("companeroMotivo", companero == null || companero.getMotivoSalida() == null ? "" : companero.getMotivoSalida().name());
            req.setAttribute("tiempoPantalla", String.format(Locale.ROOT, "%02d:%02d", (restante + 999) / 60000, ((restante + 999) / 1000) % 60));
        }
    }
    private void escribirEstado(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        resp.getWriter().printf(Locale.ROOT,
                "{\"compartidaId\":\"%s\",\"esperando\":%s,\"presente\":%s,\"motivo\":\"%s\",\"tipo\":\"%s\",\"orden\":%s,\"restanteMillis\":%s,\"objetivoMillis\":%s}",
                req.getAttribute("compartidaId"), req.getAttribute("esperando"), req.getAttribute("companeroPresente"), req.getAttribute("companeroMotivo"),
                req.getAttribute("tipoBloque"), req.getAttribute("ordenBloque"), req.getAttribute("restanteMillis"), req.getAttribute("objetivoMillis"));
    }
}
