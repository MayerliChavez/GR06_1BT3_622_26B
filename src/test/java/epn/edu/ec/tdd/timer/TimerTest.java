package epn.edu.ec.tdd.timer;

import org.junit.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class TimerTest {

    private Timer timer = null;

    @BeforeClass
    public static void setUpClass() {
        System.out.println("setUpClass()");
    }

    @Before
    public void setUp() {
        timer = new Timer();
        System.out.println("setUp()");
    }

    @Test
    public void given_valid_concentration_duration_when_configured_then_ok() {
        assertEquals(25, timer.configurarDuracionConcentracion(25));
        System.out.println("Acepta Configuraciones Válidas de Estudio");
    }

    @Test
    public void given_invalid_concentration_duration_when_configured_then_rejected() {
        assertThrows(IllegalArgumentException.class, () -> timer.configurarDuracionConcentracion(0));
        assertThrows(IllegalArgumentException.class, () -> timer.configurarDuracionConcentracion(-1));
        System.out.println("Rechaza Configuraciones fuera de rango de Estudio");
    }

    @Test
    public void given_valid_short_break_duration_when_configured_then_ok() {
        assertEquals(5, timer.configurarDuracionDescansoCorto(5));
        System.out.println("Acepta Configuraciones Válidas de Descanso Corto");
    }

    @Test
    public void given_valid_long_break_duration_when_configured_then_ok() {
        assertEquals(15, timer.configurarDuracionDescansoLargo(15));
        System.out.println("Acepta Configuraciones Válidas de Descanso Largo");
    }

    @After
    public void teardown() {
        System.out.println("teardown()");
        timer = null;
    }

    @AfterClass
    public static void tearDownClass() {
        System.out.println("tearDownClass()");
    }
}
