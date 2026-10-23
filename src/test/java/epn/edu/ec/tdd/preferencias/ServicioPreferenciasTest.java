package epn.edu.ec.tdd.preferencias;

import epn.edu.ec.model.ConfiguracionPomodoro;
import epn.edu.ec.model.SesionEstudio;
import epn.edu.ec.repository.RepositorioSesiones;
import epn.edu.ec.service.ServicioPomodoro;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import static org.junit.Assert.*;

public class ServicioPreferenciasTest {
    private RepositorioPreferencias preferencias;
    private RepositorioSesiones sesiones;
    private ServicioPreferencias servicio;

    @Before
    public void setUp() {
        preferencias = Mockito.mock(RepositorioPreferencias.class);
        sesiones = Mockito.mock(RepositorioSesiones.class);
        servicio = new ServicioPreferencias(preferencias, new ServicioPomodoro(sesiones));
    }

    @Test
    public void given_student_preferences_when_starting_session_then_use_preferences() {
        UUID estudianteId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        Instant ahora = Instant.parse("2026-10-18T15:00:00Z");
        ConfiguracionPomodoro configuracion = new ConfiguracionPomodoro(
                Duration.ofMinutes(40), Duration.ofMinutes(8), Duration.ofMinutes(20), 3);
        Mockito.when(preferencias.buscarPorEstudiante(estudianteId)).thenReturn(configuracion);

        SesionEstudio sesion = servicio.iniciarSesion(estudianteId, ahora);

        assertNotNull("Debe crearse una sesion con las preferencias del estudiante", sesion);
        assertEquals(estudianteId, sesion.getEstudiante().getId());
        assertEquals(Duration.ofMinutes(40), sesion.tiempoRestanteBloqueActual(ahora).orElseThrow());

        // Recorre bloques reales sin esperar minutos para comprobar descansos y ciclos.
        Instant fin = ahora;
        for (int bloque = 1; bloque <= 3; bloque++) {
            fin = fin.plus(Duration.ofMinutes(40));
            sesion.completarBloqueActual(fin);
            sesion.iniciarSiguienteBloque(fin);
            Duration descanso = Duration.ofMinutes(bloque == 3 ? 20 : 8);
            assertEquals(descanso, sesion.tiempoRestanteBloqueActual(fin).orElseThrow());
            if (bloque < 3) {
                fin = fin.plus(descanso);
                sesion.completarBloqueActual(fin);
                sesion.iniciarSiguienteBloque(fin);
            }
        }

        Mockito.verify(preferencias).buscarPorEstudiante(estudianteId);
        Mockito.verify(sesiones).guardar(sesion);
    }
}
