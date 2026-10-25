package epn.edu.ec.tdd.notificaciones;

import epn.edu.ec.model.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.junit.Assert.assertEquals;

public class ControladorNotificacionMockTest {

    private FormateadorNotificacion formateador;
    private ControladorNotificacion controlador;
    private UUID estudianteId;
    private SesionEstudio sesion;

    @Before
    public void setUp() {
        formateador = Mockito.mock(FormateadorNotificacion.class);
        controlador = new ControladorNotificacion(formateador);
        estudianteId = UUID.fromString("00000000-0000-0000-0000-000000000003");
        Estudiante estudiante = new Estudiante(estudianteId, "Ariel Baldeon");
        ConfiguracionPomodoro configuracion = new ConfiguracionPomodoro(
                Duration.ofMinutes(25), Duration.ofMinutes(5), Duration.ofMinutes(15), 4);
        Instant inicio = Instant.parse("2026-10-25T15:00:00Z");
        sesion = new SesionEstudio(estudiante, configuracion, inicio);
        sesion.iniciarSiguienteBloque(inicio);
        sesion.completarBloqueActual(inicio.plus(Duration.ofMinutes(25)));
        sesion.iniciarSiguienteBloque(inicio.plus(Duration.ofMinutes(25)));
    }

    @Test
    public void given_active_session_when_notifying_current_block_then_use_formatter_message() {
        String mensajeEsperado = "¡El descanso ha comenzado! Disfruta tu tiempo";
        Mockito.when(formateador.formatear(TipoBloque.DESCANSO_CORTO)).thenReturn(mensajeEsperado);

        String mensaje = controlador.generarNotificacion(sesion, estudianteId);

        assertEquals(mensajeEsperado, mensaje);
        Mockito.verify(formateador).formatear(TipoBloque.DESCANSO_CORTO);
    }

    @After
    public void tearDown() {
        sesion = null;
        estudianteId = null;
        controlador = null;
        formateador = null;
    }
}
