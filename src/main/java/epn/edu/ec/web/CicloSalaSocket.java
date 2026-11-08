package epn.edu.ec.web;

import epn.edu.ec.service.ServicioCicloSalaPrivada;
import jakarta.servlet.http.HttpSession;
import jakarta.websocket.*;
import jakarta.websocket.server.*;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

@ServerEndpoint(
        value = "/salas/ciclo/{salaId}",
        configurator = ChatSocket.Autenticacion.class
)
public class CicloSalaSocket {
    private static final Map<String, CicloSalaSocket> CONEXIONES =
            new ConcurrentHashMap<>();

    private final SerializadorCicloSala serializador =
            new SerializadorCicloSala();

    private final AtomicBoolean enviando =
            new AtomicBoolean(false);

    private Session socket;
    private HttpSession http;
    private UUID estudianteId;
    private UUID salaId;
    private ServicioCicloSalaPrivada servicio;

    @OnOpen
    public void abrir(
            Session socket,
            EndpointConfig config,
            @PathParam("salaId") String id
    ) {
        this.socket = socket;

        socket.setMaxTextMessageBufferSize(64);
        socket.setMaxIdleTimeout(90000);
        socket.getAsyncRemote().setSendTimeout(5000);

        try {
            http = (HttpSession) config.getUserProperties()
                    .get("httpSession");

            if (http == null
                    || http.getAttribute("cuentaId") == null) {
                throw new SecurityException();
            }

            estudianteId =
                    (UUID) http.getAttribute("estudianteId");

            if (estudianteId == null) {
                throw new SecurityException();
            }

            String csrf =
                    (String) http.getAttribute("csrf");

            if (csrf == null
                    || !List.of(csrf).equals(
                    socket.getRequestParameterMap()
                            .get("csrf")
            )) {
                throw new SecurityException();
            }

            if (!Boolean.TRUE.equals(
                    config.getUserProperties().get("sameOrigin")
            )) {
                throw new SecurityException();
            }

            salaId = UUID.fromString(id);

            servicio = (ServicioCicloSalaPrivada)
                    http.getServletContext().getAttribute(
                            "servicioCicloSalaPrivada"
                    );

            if (servicio == null) {
                throw new IllegalStateException(
                        "El ciclo no está disponible."
                );
            }

            // Verifica la pertenencia antes de registrar la conexión.
            var estado = servicio.consultar(
                    estudianteId,
                    Instant.now()
            );

            if (!salaId.equals(estado.salaId())) {
                throw new SecurityException();
            }

            CONEXIONES.put(socket.getId(), this);
            publicarEstado();

        } catch (RuntimeException error) {
            cerrar(
                    CloseReason.CloseCodes.VIOLATED_POLICY,
                    "Acceso a la sala no autorizado."
            );
        }
    }

    @OnMessage
    public void recibir(String texto) {
        if (!"P".equals(texto)) {
            cerrar(
                    CloseReason.CloseCodes.VIOLATED_POLICY,
                    "Mensaje no permitido."
            );
            return;
        }

        publicarEstado();
    }

    private void comprobarIdentidad() {
        if (http == null
                || http.getAttribute("cuentaId") == null
                || !estudianteId.equals(
                http.getAttribute("estudianteId")
        )) {
            throw new SecurityException();
        }
    }

    private void publicarEstado() {
        if (socket == null
                || !socket.isOpen()
                || !enviando.compareAndSet(false, true)) {
            return;
        }

        try {
            comprobarIdentidad();

            var estado = servicio.consultar(
                    estudianteId,
                    Instant.now()
            );

            if (!salaId.equals(estado.salaId())) {
                throw new SecurityException();
            }

            socket.getAsyncRemote().sendText(
                    serializador.serializar(estado),
                    resultado -> {
                        enviando.set(false);

                        if (!resultado.isOK()) {
                            cerrar(
                                    CloseReason.CloseCodes.UNEXPECTED_CONDITION,
                                    "No se pudo enviar el estado."
                            );
                        }
                    }
            );

        } catch (SecurityException | IllegalStateException error) {
            enviando.set(false);

            cerrar(
                    CloseReason.CloseCodes.VIOLATED_POLICY,
                    "La sala ya no está disponible."
            );

        } catch (RuntimeException error) {
            enviando.set(false);

            Logger.getLogger(
                    CicloSalaSocket.class.getName()
            ).log(
                    Level.WARNING,
                    "No se pudo comunicar el ciclo de la sala.",
                    error
            );

            cerrar(
                    CloseReason.CloseCodes.UNEXPECTED_CONDITION,
                    "No se pudo consultar el ciclo."
            );
        }
    }

    public static void publicarEstados() {
        for (var conexion : CONEXIONES.values()) {
            conexion.publicarEstado();
        }
    }

    public static void cerrarTodas() {
        for (var conexion : CONEXIONES.values()) {
            conexion.cerrar(
                    CloseReason.CloseCodes.GOING_AWAY,
                    "El servidor se está deteniendo."
            );
        }
    }

    private void cerrar(
            CloseReason.CloseCode codigo,
            String motivo
    ) {
        if (socket == null) {
            return;
        }

        CONEXIONES.remove(socket.getId(), this);

        try {
            if (socket.isOpen()) {
                socket.close(new CloseReason(codigo, motivo));
            }
        } catch (IOException | IllegalStateException ignored) {
        }
    }

    @OnClose
    public void salir() {
        if (socket != null) {
            CONEXIONES.remove(socket.getId(), this);
        }
    }

    @OnError
    public void error(Throwable error) {
        cerrar(
                CloseReason.CloseCodes.UNEXPECTED_CONDITION,
                "La conexión se interrumpió."
        );
    }
}