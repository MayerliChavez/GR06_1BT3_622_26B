package epn.edu.ec.web;

import epn.edu.ec.model.MensajeChat;
import epn.edu.ec.service.ServicioChat;
import jakarta.servlet.http.HttpSession;
import jakarta.websocket.*;
import jakarta.websocket.server.*;
import java.io.IOException;
import java.net.URI;
import java.util.*;

@ServerEndpoint(value = "/chat/{sesionId}", configurator = ChatSocket.Autenticacion.class)
public class ChatSocket {
    private HttpSession http;
    private Session socket;
    private UUID sesionId;
    private UUID estudianteId;
    private ServicioChat servicio;
    private Object bloqueo;
    private Map<String, ChatSocket> conexiones;

    public static class Autenticacion extends ServerEndpointConfig.Configurator {
        @Override public void modifyHandshake(ServerEndpointConfig config, HandshakeRequest request,
                HandshakeResponse response) {
            config.getUserProperties().put("httpSession", request.getHttpSession());
            String origin = request.getHeaders().getOrDefault("Origin",
                    request.getHeaders().getOrDefault("origin", List.of(""))).get(0);
            boolean sameOrigin = false;
            try {
                URI source = URI.create(origin);
                URI target = request.getRequestURI();
                int sourcePort = source.getPort() == -1 ? ("https".equals(source.getScheme()) ? 443 : 80) : source.getPort();
                int targetPort = target.getPort() == -1 ? ("wss".equals(target.getScheme()) || "https".equals(target.getScheme()) ? 443 : 80) : target.getPort();
                sameOrigin = Objects.equals(source.getHost(), target.getHost()) && sourcePort == targetPort
                        && ("https".equals(source.getScheme()) == ("wss".equals(target.getScheme()) || "https".equals(target.getScheme())));
            } catch (IllegalArgumentException ignored) {}
            config.getUserProperties().put("sameOrigin", sameOrigin);
        }
    }

    @OnOpen
    @SuppressWarnings("unchecked")
    public void abrir(Session socket, EndpointConfig config, @PathParam("sesionId") String id) throws IOException {
        this.socket = socket;
        socket.setMaxTextMessageBufferSize(8192);
        socket.setMaxIdleTimeout(120000);
        socket.getAsyncRemote().setSendTimeout(5000);
        try {
            http = (HttpSession) config.getUserProperties().get("httpSession");
            if (http == null || http.getAttribute("cuentaId") == null) throw new SecurityException();
            estudianteId = (UUID) http.getAttribute("estudianteId");
            String csrf = (String) http.getAttribute("csrf");
            if (csrf == null || !List.of(csrf).equals(socket.getRequestParameterMap().get("csrf")))
                throw new SecurityException();
            if (!Boolean.TRUE.equals(config.getUserProperties().get("sameOrigin"))) throw new SecurityException();
            sesionId = UUID.fromString(id);
            var contexto = http.getServletContext();
            servicio = (ServicioChat) contexto.getAttribute("servicioChat");
            bloqueo = contexto.getAttribute("servicioCompartido");
            conexiones = (Map<String, ChatSocket>) contexto.getAttribute("conexionesChat");
            synchronized (bloqueo) {
                var historial = servicio.historial(sesionId, estudianteId);
                conexiones.put(socket.getId(), this);
                enviar("{\"tipo\":\"historial\",\"mensajes\":[" +
                        String.join(",", historial.stream().map(ChatSocket::json).toList()) + "]}");
            }
        } catch (IllegalArgumentException | IllegalStateException | SecurityException e) {
            socket.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, "Acceso al chat no autorizado."));
        }
    }

    @OnMessage
    public void recibir(String texto) throws IOException {
        if (servicio == null || conexiones == null) return;
        synchronized (bloqueo) {
            try {
                comprobarIdentidad();
                if ("P".equals(texto)) {
                    servicio.exigirParticipante(sesionId, estudianteId);
                    enviar("{\"tipo\":\"pong\"}");
                    return;
                }
                if (!texto.startsWith("M")) throw new IllegalArgumentException("Formato de mensaje incorrecto.");
                var mensaje = servicio.registrar(sesionId, estudianteId, texto.substring(1));
                String evento = "{\"tipo\":\"mensaje\",\"mensaje\":" + json(mensaje) + "}";
                for (ChatSocket destino : conexiones.values()) {
                    if (!sesionId.equals(destino.sesionId)) continue;
                    try {
                        destino.comprobarIdentidad();
                        servicio.exigirParticipante(sesionId, destino.estudianteId);
                        destino.enviar(evento);
                    } catch (IllegalStateException | SecurityException e) {
                        destino.cerrar();
                    }
                }
            } catch (IllegalArgumentException e) {
                enviar("{\"tipo\":\"error\",\"mensaje\":" + cadena(e.getMessage()) + "}");
            } catch (IllegalStateException | SecurityException e) {
                cerrar();
            } catch (RuntimeException e) {
                http.getServletContext().log("No se pudo registrar el mensaje del chat.", e);
                enviar("{\"tipo\":\"error\",\"mensaje\":\"No se pudo guardar el mensaje. Intenta de nuevo.\"}");
            }
        }
    }

    private void comprobarIdentidad() {
        if (http.getAttribute("cuentaId") == null || !estudianteId.equals(http.getAttribute("estudianteId")))
            throw new SecurityException();
    }
    private void enviar(String texto) {
        if (socket.isOpen()) socket.getAsyncRemote().sendText(texto, resultado -> {
            if (!resultado.isOK()) cerrar();
        });
    }
    private void cerrar() {
        try { socket.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, "La sesion del chat ya no esta disponible.")); }
        catch (IOException ignored) {}
        if (conexiones != null) conexiones.remove(socket.getId());
    }
    @OnClose public void salir() { if (conexiones != null) conexiones.remove(socket.getId()); }
    @OnError public void error(Throwable error) { cerrar(); }

    private static String json(MensajeChat m) {
        return "{\"id\":" + m.getId() + ",\"texto\":" + cadena(m.getTexto()) +
                ",\"autorId\":" + cadena(m.getAutorId().toString()) +
                ",\"autor\":" + cadena(m.getNombreAutor()) +
                ",\"fecha\":" + cadena(m.getFecha().toString()) + "}";
    }
    private static String cadena(String valor) {
        StringBuilder resultado = new StringBuilder("\"");
        for (char c : valor.toCharArray()) {
            switch (c) {
                case '"' -> resultado.append("\\\"");
                case '\\' -> resultado.append("\\\\");
                case '\n' -> resultado.append("\\n");
                case '\r' -> resultado.append("\\r");
                case '\t' -> resultado.append("\\t");
                default -> {
                    if (c < 32) resultado.append(String.format("\\u%04x", (int)c));
                    else resultado.append(c);
                }
            }
        }
        return resultado.append('"').toString();
    }
}
