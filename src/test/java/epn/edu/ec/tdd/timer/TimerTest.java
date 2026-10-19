package epn.edu.ec.tdd.timer;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class TimerTest {

    @Test
    public void given_valid_concentration_duration_when_configured_then_ok() {
        Timer timer = new Timer();
        assertEquals(25, timer.configurarDuracionConcentracion(25));
        System.out.println("Acepta Configuraciones Válidas de Estudio");
    }

    @Test
    public void given_invalid_concentration_duration_when_configured_then_rejected() {
        Timer timer = new Timer();
        assertThrows(IllegalArgumentException.class, () -> timer.configurarDuracionConcentracion(0));
        assertThrows(IllegalArgumentException.class, () -> timer.configurarDuracionConcentracion(-1));
        System.out.println("Rechaza Configuraciones fuera de rango de Estudio");
    }

}
