package epn.edu.ec.tdd.rachas;

import epn.edu.ec.model.ConfiguracionPomodoro;
import epn.edu.ec.model.Estudiante;
import epn.edu.ec.model.SesionEstudio;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.Before;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ControladorRachaTest {

    private ControladorRacha controlador;
    private SesionEstudio sesion;

    @Before
    public void setUp() {
        controlador = new ControladorRacha();
        Estudiante estudiante = new Estudiante(
                UUID.fromString("00000000-0000-0000-0000-000000000001"), "Ariel");
        ConfiguracionPomodoro configuracion = new ConfiguracionPomodoro(
                Duration.ofMinutes(25), Duration.ofMinutes(5), Duration.ofMinutes(15), 4);
        Instant inicio = Instant.parse("2026-10-25T15:00:00Z");
        sesion = new SesionEstudio(estudiante, configuracion, inicio);
        sesion.iniciarSiguienteBloque(inicio);
    }

    @Test
    public void given_incomplete_session_when_updating_streak_then_keep_current_days() {
        int diasActuales = 3;
        LocalDate ultimoDiaEstudio = LocalDate.of(2026, 10, 24);

        int actual = controlador.actualizarRacha(sesion, diasActuales, ultimoDiaEstudio);

        assertEquals("Una sesion no completada debe conservar los dias de la racha", diasActuales, actual);
    }
    @After
    public void tearDown() {
        controlador = null;
        sesion = null;
    }
}
