package epn.edu.ec.tdd.rachas;

import epn.edu.ec.model.*;
import java.time.*;

public class ControladorRacha {
    public static final ZoneId ZONA_ESTUDIO = ZoneId.of("America/Bogota");
    private final EvaluadorRachas evaluador = new EvaluadorRachas();

    public boolean esSesionCompletada(SesionEstudio sesion) {
        return sesion.getBloques().stream().anyMatch(b ->
                b.getTipo() == TipoBloque.CONCENTRACION && b.getEstado() == EstadoBloque.COMPLETADO);
    }

    public int actualizarRacha(SesionEstudio sesion, int diasActuales, LocalDate ultimoDiaEstudio) {
        if (!esSesionCompletada(sesion)) return diasActuales;
        return evaluador.calcularRacha(diasActuales, ultimoDiaEstudio, fechaEstudio(sesion));
    }

    public LocalDate fechaEstudio(SesionEstudio sesion) {
        return sesion.getBloques().stream()
                .filter(b -> b.getTipo() == TipoBloque.CONCENTRACION && b.getEstado() == EstadoBloque.COMPLETADO)
                .map(BloquePomodoro::getFechaFin).max(Instant::compareTo).orElseThrow()
                .atZone(ZONA_ESTUDIO).toLocalDate();
    }
}
