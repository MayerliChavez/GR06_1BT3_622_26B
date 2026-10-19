package epn.edu.ec.tdd.timer;

import epn.edu.ec.model.ConfiguracionPomodoro;
import epn.edu.ec.model.TipoBloque;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

@RunWith(Parameterized.class)
public class TimerParametersTest {

    private final int concentracion;
    private final int descansoCorto;
    private final int descansoLargo;
    private final int bloques;
    private final boolean valida;

    @Parameterized.Parameters(name = "{index}: concentracion={0}, corto={1}, largo={2}, bloques={3}, valida={4}")
    public static Iterable<Object[]> parameters() {
        List<Object[]> casos = new ArrayList<>();

        // Valores validos, incluyendo el minimo permitido: 1.
        casos.add(new Object[]{25, 5, 15, 4, true});
        casos.add(new Object[]{50, 10, 30, 3, true});
        casos.add(new Object[]{1, 1, 1, 4, true});
        casos.add(new Object[]{40, 8, 20, 6, true});

        // Cada parametro se prueba con cero y con un valor negativo.
        casos.add(new Object[]{0, 5, 15, 4, false});
        casos.add(new Object[]{-1, 5, 15, 4, false});
        casos.add(new Object[]{25, 0, 15, 4, false});
        casos.add(new Object[]{25, -1, 15, 4, false});
        casos.add(new Object[]{25, 5, 0, 4, false});
        casos.add(new Object[]{25, 5, -1, 4, false});
        casos.add(new Object[]{25, 5, 15, 0, false});
        casos.add(new Object[]{25, 5, 15, -1, false});

        return casos;
    }

    public TimerParametersTest(int concentracion, int descansoCorto,
                               int descansoLargo, int bloques, boolean valida) {
        this.concentracion = concentracion;
        this.descansoCorto = descansoCorto;
        this.descansoLargo = descansoLargo;
        this.bloques = bloques;
        this.valida = valida;
    }

    @Test
    public void given_parameters_when_configured_then_accept_or_reject() {
        if (!valida) {
            assertThrows(IllegalArgumentException.class, () ->
                    new ConfiguracionPomodoro(Duration.ofMinutes(concentracion),
                            Duration.ofMinutes(descansoCorto),
                            Duration.ofMinutes(descansoLargo), bloques));
            return;
        }

        ConfiguracionPomodoro configuracion = new ConfiguracionPomodoro(
                Duration.ofMinutes(concentracion), Duration.ofMinutes(descansoCorto),
                Duration.ofMinutes(descansoLargo), bloques);

        assertEquals(Duration.ofMinutes(concentracion), configuracion.duracionPara(TipoBloque.CONCENTRACION));
        assertEquals(Duration.ofMinutes(descansoCorto), configuracion.duracionPara(TipoBloque.DESCANSO_CORTO));
        assertEquals(Duration.ofMinutes(descansoLargo), configuracion.duracionPara(TipoBloque.DESCANSO_LARGO));

        for (int completados = 1; completados < bloques; completados++) {
            assertEquals(TipoBloque.DESCANSO_CORTO, configuracion.tipoDescansoTras(completados));
        }
        assertEquals(TipoBloque.DESCANSO_LARGO, configuracion.tipoDescansoTras(bloques));
        assertEquals(TipoBloque.DESCANSO_LARGO, configuracion.tipoDescansoTras(2 * bloques));
    }

}
