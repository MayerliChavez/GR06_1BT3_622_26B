package epn.edu.ec.service;

import epn.edu.ec.model.TipoBloque;

public class PoliticaAdmisionSala {

    public boolean puedeAceptar(
            TipoBloque bloque,
            int participantes,
            boolean esResponsable) {

        boolean hayCupo = participantes >= 1 && participantes < 4;

        boolean momentoPermitido = bloque == null
                || bloque == TipoBloque.DESCANSO_CORTO
                || bloque == TipoBloque.DESCANSO_LARGO;

        return esResponsable && hayCupo && momentoPermitido;
    }
}
