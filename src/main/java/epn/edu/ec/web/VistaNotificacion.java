package epn.edu.ec.web;

import epn.edu.ec.model.SesionEstudio;
import epn.edu.ec.tdd.notificaciones.*;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;

final class VistaNotificacion {
    private static final ControladorNotificacion CONTROLADOR =
            new ControladorNotificacion(new FormateadorNotificacion());
    private VistaNotificacion() {}

    static void preparar(HttpServletRequest req, SesionEstudio sesion, UUID estudianteId) {
        String json = CONTROLADOR.crearNotificacion(sesion, estudianteId).map(n ->
                "{\"id\":\"" + n.id() + "\",\"mensaje\":\"" + escapar(n.mensaje())
                + "\",\"posicion\":\"" + n.posicion() + "\",\"duracionMillis\":" + n.duracionMillis()
                + ",\"tipoBloque\":\"" + n.tipoBloque().name() + "\",\"duracionBloqueMillis\":" + n.duracionBloqueMillis() + "}")
                .orElse("null");
        req.setAttribute("notificacionJson", json);
    }

    private static String escapar(String texto) {
        return texto.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r");
    }
}
