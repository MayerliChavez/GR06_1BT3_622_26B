package epn.edu.ec.tdd.timer;

public class Timer {

    public int configurarDuracionConcentracion(int minutos) {
        if (minutos <= 0) {
            throw new IllegalArgumentException("La duración de concentración debe ser mayor que cero");
        }
        return minutos;
    }

    public int configurarDuracionDescansoCorto(int minutos) {
        return minutos;
    }


}
