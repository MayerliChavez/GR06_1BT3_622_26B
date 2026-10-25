package epn.edu.ec.tdd.notificaciones;

import epn.edu.ec.model.*;
import java.util.*;

public class ControladorNotificacion {

    private final FormateadorNotificacion formateador;

    public ControladorNotificacion(FormateadorNotificacion formateador) {
        this.formateador = Objects.requireNonNull(formateador);
    }

    public String generarNotificacion(SesionEstudio sesion, UUID estudianteId) {
        if (!puedeNotificar(sesion, estudianteId)) return "";
        return formateador.formatear(bloqueActual(sesion).getTipo());
    }

    // Maneja el tiempo en el que se presenta la notificación.
    public Optional<NotificacionFlotante> crearNotificacion(SesionEstudio sesion, UUID estudianteId) {
        String mensaje = generarNotificacion(sesion, estudianteId);
        if (mensaje.isEmpty()) return Optional.empty();
        return Optional.of(new NotificacionFlotante(sesion.getId() + ":" + bloqueActual(sesion).getOrden(),
                estudianteId, mensaje, "CENTRO_APLICACION", 10500,
                bloqueActual(sesion).getTipo(), bloqueActual(sesion).getDuracionObjetivo().toMillis()));
    }

    private boolean puedeNotificar(SesionEstudio sesion, UUID estudianteId) {
        return sesion != null && sesion.getEstado() == EstadoSesion.ACTIVA
                && sesion.getEstudiante().getId().equals(estudianteId) && !sesion.getBloques().isEmpty()
                && bloqueActual(sesion).getEstado() == EstadoBloque.EN_EJECUCION;
    }

    private BloquePomodoro bloqueActual(SesionEstudio sesion) {
        var bloques = sesion.getBloques();
        return bloques.get(bloques.size() - 1);
    }
    
}
