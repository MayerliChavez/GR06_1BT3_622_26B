package edu.proyecto.web;

import edu.proyecto.model.*;
import edu.proyecto.repository.RepositorioSesiones;
import edu.proyecto.repository.RepositorioSesionesCompartidas;
import edu.proyecto.service.ServicioPomodoro;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** Controlador técnico de las pantallas de robustez. No es una entidad del dominio. */
@WebServlet({"/pomodoro", "/historial", "/estado"})
public class PomodoroServlet extends HttpServlet {
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd MMM yyyy · HH:mm", new Locale("es", "CO"))
            .withZone(ZoneId.of("America/Bogota"));

    @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (req.getServletPath().equals("/estado")) {
            resp.sendRedirect(req.getContextPath() + "/pomodoro");
            return;
        }
        resp.setHeader("Cache-Control", "no-store");
        resp.setContentType("text/html;charset=UTF-8");
        UUID estudianteId = identificarEstudiante(req, resp);
        req.setAttribute("csrf", tokenCsrf(req));
        prepararVista(req, estudianteId);
        if ("json".equals(req.getParameter("formato"))) {
            escribirEstado(req, resp);
            return;
        }
        var mensaje = req.getSession().getAttribute("mensaje");
        req.getSession().removeAttribute("mensaje");
        req.setAttribute("mensaje", mensaje);
        req.setAttribute("esHistorial", req.getServletPath().equals("/historial"));
        req.getRequestDispatcher("/WEB-INF/views/pomora.jsp").forward(req, resp);
    }

    @Override protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setHeader("Cache-Control", "no-store");
        UUID estudianteId = identificarEstudiante(req, resp);
        if (!tokenCsrf(req).equals(req.getParameter("csrf"))) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Recarga la página antes de continuar.");
            return;
        }
        var servicio = (ServicioPomodoro) getServletContext().getAttribute("servicioPomodoro");
        var repositorio = (RepositorioSesiones) getServletContext().getAttribute("repositorioSesiones");
        try {
            synchronized (getServletContext().getAttribute("servicioCompartido")) { synchronized (servicio) {
                Instant ahora = Instant.now();
                String accion = Objects.toString(req.getParameter("accion"), "");
                var compartidas = (RepositorioSesionesCompartidas) getServletContext().getAttribute("repositorioCompartidas");
                if (compartidas.buscarAbiertaPorEstudiante(estudianteId).isPresent())
                    throw new IllegalStateException("Usa la pantalla compartida para gestionar esta sesión sincronizada.");
                ((edu.proyecto.service.ServicioEstudioCompartido) getServletContext().getAttribute("servicioCompartido")).cancelarEspera(estudianteId);
                ((PresenciaCompartida) getServletContext().getAttribute("presenciaCompartida")).olvidar(estudianteId);
                req.getSession().setAttribute("esperando", false);
                if (accion.equals("iniciar")) {
                    servicio.iniciarSesion(estudianteId,
                            new ConfiguracionPomodoro(Duration.ofMinutes(25), Duration.ofMinutes(5), Duration.ofMinutes(15), 4), ahora);
                } else {
                    UUID sesionId = UUID.fromString(Objects.toString(req.getParameter("sesionId"), ""));
                    var sesion = repositorio.buscarPorId(sesionId).orElseThrow(() -> new IllegalArgumentException("Sesión inexistente."));
                    if (!sesion.getEstudiante().getId().equals(estudianteId)) {
                        resp.sendError(HttpServletResponse.SC_FORBIDDEN);
                        return;
                    }
                    switch (accion) {
                        case "pausar" -> servicio.pausarTemporizador(sesionId, ahora);
                        case "reanudar" -> servicio.reanudarTemporizador(sesionId, ahora);
                        case "completar" -> servicio.completarBloqueActual(sesionId, ahora);
                        case "siguiente" -> servicio.iniciarSiguienteBloque(sesionId, ahora);
                        case "finalizar" -> servicio.finalizarSesion(sesionId, ahora);
                        case "abandonar" -> servicio.abandonarSesion(sesionId, ahora);
                        default -> throw new IllegalArgumentException("Acción desconocida.");
                    }
                }
            } }
        } catch (IllegalStateException | IllegalArgumentException e) {
            if ("json".equals(req.getParameter("formato"))) {
                resp.setStatus(HttpServletResponse.SC_CONFLICT);
                prepararVista(req, estudianteId);
                escribirEstado(req, resp);
                return;
            }
            req.getSession().setAttribute("mensaje", e.getMessage());
        }
        if ("json".equals(req.getParameter("formato"))) {
            prepararVista(req, estudianteId);
            escribirEstado(req, resp);
        } else resp.sendRedirect(req.getContextPath() + "/pomodoro");
    }

    private UUID identificarEstudiante(HttpServletRequest req, HttpServletResponse resp) {
        return IdentidadWeb.estudiante(req, resp);
    }

    private String tokenCsrf(HttpServletRequest req) {
        var session = req.getSession();
        String token = (String) session.getAttribute("csrf");
        if (token == null) { token = UUID.randomUUID().toString(); session.setAttribute("csrf", token); }
        return token;
    }

    private void prepararVista(HttpServletRequest req, UUID estudianteId) {
        var servicio = (ServicioPomodoro) getServletContext().getAttribute("servicioPomodoro");
        var repositorio = (RepositorioSesiones) getServletContext().getAttribute("repositorioSesiones");
        synchronized (servicio) {
            Instant ahora = Instant.now();
            var activa = repositorio.buscarActivaPorEstudiante(estudianteId).orElse(null);
            BloquePomodoro bloque = activa == null || activa.getBloques().isEmpty() ? null
                    : activa.getBloques().get(activa.getBloques().size() - 1);
            long restante = activa == null ? Duration.ofMinutes(25).toMillis()
                    : servicio.consultarTiempoRestante(activa.getId(), ahora).orElse(Duration.ZERO).toMillis();
            req.setAttribute("sesionId", activa == null ? "" : activa.getId().toString());
            req.setAttribute("estadoBloque", bloque == null ? "LISTO" : bloque.getEstado().name());
            req.setAttribute("tipoBloque", bloque == null ? TipoBloque.CONCENTRACION.name() : bloque.getTipo().name());
            req.setAttribute("ordenBloque", bloque == null ? 1 : bloque.getOrden());
            req.setAttribute("restanteMillis", restante);
            req.setAttribute("objetivoMillis", bloque == null ? Duration.ofMinutes(25).toMillis() : bloque.getDuracionObjetivo().toMillis());
            req.setAttribute("tiempoPantalla", formatearTiempo(Duration.ofMillis(restante)));
            req.setAttribute("hayActiva", activa != null);
            req.setAttribute("ahoraEpoch", ahora.toEpochMilli());
            List<Map<String, Object>> filas = new ArrayList<>();
            Duration total = Duration.ZERO;
            long completados = 0;
            for (SesionEstudio sesion : servicio.consultarHistorial(estudianteId)) {
                var tiempo = sesion.tiempoEstudiado(ahora);
                total = total.plus(tiempo);
                Map<String, Object> fila = new LinkedHashMap<>();
                fila.put("id", sesion.getId().toString());
                fila.put("inicio", FECHA.format(sesion.getFechaInicio()));
                fila.put("fin", sesion.getFechaFin() == null ? "En curso" : FECHA.format(sesion.getFechaFin()));
                fila.put("estado", sesion.getEstado().name());
                fila.put("estudiado", formatearTiempo(tiempo));
                List<Map<String, Object>> bloques = new ArrayList<>();
                for (BloquePomodoro b : sesion.getBloques()) {
                    if (b.getTipo() == TipoBloque.CONCENTRACION && b.getEstado() == EstadoBloque.COMPLETADO) completados++;
                    bloques.add(Map.of("orden", b.getOrden(), "tipo", b.getTipo().name(), "estado", b.getEstado().name(),
                            "tiempo", formatearTiempo(b.tiempoActivo(ahora)), "inicio", FECHA.format(b.getFechaInicio()),
                            "fin", b.getFechaFin() == null ? "En curso" : FECHA.format(b.getFechaFin())));
                }
                fila.put("bloques", bloques);
                filas.add(fila);
            }
            req.setAttribute("historial", filas);
            req.setAttribute("totalSesiones", filas.size());
            req.setAttribute("totalEstudiado", formatearTiempo(total));
            req.setAttribute("concentracionesCompletadas", completados);
        }
    }

    private void escribirEstado(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        // Solo UUID, enumeraciones y números generados por el servidor.
        resp.getWriter().printf(Locale.ROOT,
                "{\"sesionId\":\"%s\",\"estado\":\"%s\",\"tipo\":\"%s\",\"orden\":%s,\"restanteMillis\":%s,\"objetivoMillis\":%s,\"ahoraEpoch\":%s}",
                req.getAttribute("sesionId"), req.getAttribute("estadoBloque"), req.getAttribute("tipoBloque"),
                req.getAttribute("ordenBloque"), req.getAttribute("restanteMillis"), req.getAttribute("objetivoMillis"), req.getAttribute("ahoraEpoch"));
    }

    private String formatearTiempo(Duration tiempo) {
        long segundos = (tiempo.toMillis() + 999) / 1000;
        return String.format(Locale.ROOT, "%02d:%02d", segundos / 60, segundos % 60);
    }
}
