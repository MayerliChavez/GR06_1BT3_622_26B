package epn.edu.ec.tdd.timer;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TimerTest {

    @Test
    public void given_valid_concentration_duration_when_configured_then_ok() {
        Timer timer = new Timer();
        assertEquals(25, timer.configurarDuracionConcentracion(25));
        System.out.println("Acepta Configuraciones Validas");
    }
}
