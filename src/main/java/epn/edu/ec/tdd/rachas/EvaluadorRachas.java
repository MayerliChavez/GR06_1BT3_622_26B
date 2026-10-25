package epn.edu.ec.tdd.rachas;

import java.time.LocalDate;

public class EvaluadorRachas {

    public int calcularRacha(int diasActuales, LocalDate ultimoDiaEstudio, LocalDate fechaFinalizacion) {
        if (sinRachaPrevia(diasActuales, ultimoDiaEstudio)) {
            return 1;
        }
        if (fechaFinalizacion.equals(ultimoDiaEstudio)) {
            return diasActuales;
        }
        if (esDiaConsecutivo(ultimoDiaEstudio, fechaFinalizacion)) {
            return diasActuales + 1;
        }
        return 1;
    }

    private boolean sinRachaPrevia(int diasActuales, LocalDate ultimoDiaEstudio) {
        return ultimoDiaEstudio == null || diasActuales == 0;
    }

    private boolean esDiaConsecutivo(LocalDate ultimoDiaEstudio, LocalDate fechaFinalizacion) {
        return fechaFinalizacion.equals(ultimoDiaEstudio.plusDays(1));
    }
}
