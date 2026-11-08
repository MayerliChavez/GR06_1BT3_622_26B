package epn.edu.ec.service;

import epn.edu.ec.model.TipoBloque;

public class PoliticaAdmisionSala {
    public static final int MAX_PARTICIPANTES = 4;

    public record Decision(boolean permitida, String mensaje) {}

    public Decision evaluar(TipoBloque bloque, int participantes, boolean esResponsable) {
        if (!esResponsable) {
            return new Decision(false, "Solo el responsable puede atender solicitudes.");
        }
        if (participantes < 1 || participantes >= MAX_PARTICIPANTES) {
            return new Decision(false, "La sala está llena. No es posible aceptar más participantes.");
        }
        if (bloque == TipoBloque.CONCENTRACION) {
            return new Decision(false, "Podrás aceptar nuevos participantes en el siguiente descanso.");
        }
        return new Decision(true, "");
    }

    public void exigirAceptacion(TipoBloque bloque, int participantes, boolean esResponsable) {
        var decision = evaluar(bloque, participantes, esResponsable);
        if (!decision.permitida()) {
            if (!esResponsable) throw new SecurityException(decision.mensaje());
            throw new IllegalStateException(decision.mensaje());
        }
    }

    public boolean puedeAceptar(
            TipoBloque bloque,
            int participantes,
            boolean esResponsable) {

        return evaluar(bloque, participantes, esResponsable).permitida();
    }
}
