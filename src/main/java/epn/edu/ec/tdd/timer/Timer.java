package epn.edu.ec.tdd.timer;

import epn.edu.ec.model.ConfiguracionPomodoro;
import java.time.Duration;

public class Timer {
    public int configurarDuracionConcentracion(int minutos) { return exigirPositivo(minutos); }
    public int configurarDuracionDescansoCorto(int minutos) { return exigirPositivo(minutos); }
    public int configurarDuracionDescansoLargo(int minutos) { return exigirPositivo(minutos); }

    public ConfiguracionPomodoro configurar(int concentracion, int corto, int largo, int bloques) {
        return new ConfiguracionPomodoro(Duration.ofMinutes(configurarDuracionConcentracion(concentracion)),
                Duration.ofMinutes(configurarDuracionDescansoCorto(corto)),
                Duration.ofMinutes(configurarDuracionDescansoLargo(largo)), exigirPositivo(bloques));
    }

    private int exigirPositivo(int valor) {
        if (valor <= 0) throw new IllegalArgumentException("Las duraciones y la cantidad de bloques deben ser enteros mayores que cero.");
        return valor;
    }
}
