package epn.edu.ec.tdd.notificaciones;

import epn.edu.ec.model.TipoBloque;

public class FormateadorNotificacion {

    private static final String MENSAJE_CONCENTRACION = "Es hora de concentrarse. ¡Mucho éxito!";
    private static final String MENSAJE_DESCANSO = "¡El descanso ha comenzado! Disfruta tu tiempo";

    public String formatear(TipoBloque tipoBloque) {
        if (tipoBloque == TipoBloque.CONCENTRACION) {
            return MENSAJE_CONCENTRACION;
        }
        return MENSAJE_DESCANSO;
    }

}
