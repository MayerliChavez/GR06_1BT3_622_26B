package edu.proyecto;

import edu.proyecto.model.*;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class PomodoroDominioTest {
    private final Instant inicio = Instant.parse("2026-10-04T00:00:00Z");

    @Test void pausaConservaTiempoYReanudarNoCuentaLaPausa() {
        var b = new BloquePomodoro(1, TipoBloque.CONCENTRACION, Duration.ofMinutes(25), inicio);
        b.pausar(inicio.plusSeconds(60));
        assertEquals(EstadoBloque.PAUSADO, b.getEstado());
        assertEquals(Duration.ofMinutes(24), b.tiempoRestante(inicio.plusSeconds(3600)));
        b.reanudar(inicio.plusSeconds(3600));
        assertEquals(Duration.ofMinutes(23), b.tiempoRestante(inicio.plusSeconds(3660)));
        b.pausar(inicio.plusSeconds(3660));
        b.reanudar(inicio.plusSeconds(7200));
        assertEquals(Duration.ofMinutes(3), b.tiempoActivo(inicio.plusSeconds(7260)));
    }

    @Test void completarNoAgregaTiempoDespuesDelVencimiento() {
        var b = new BloquePomodoro(1, TipoBloque.CONCENTRACION, Duration.ofSeconds(10), inicio);
        b.pausar(inicio.plusSeconds(4));
        b.reanudar(inicio.plusSeconds(100));
        assertThrows(IllegalStateException.class, () -> b.completar(inicio.plusSeconds(105)));
        b.completar(inicio.plusSeconds(999));
        assertEquals(inicio.plusSeconds(106), b.getFechaFin());
        assertEquals(Duration.ofSeconds(10), b.tiempoActivo(inicio.plusSeconds(2000)));
        assertEquals(Duration.ZERO, b.tiempoRestante(inicio.plusSeconds(2000)));
        assertThrows(IllegalStateException.class, () -> b.reanudar(inicio.plusSeconds(2000)));
        assertThrows(IllegalStateException.class, () -> b.cancelar(inicio.plusSeconds(2000)));
    }

    @Test void rechazaTransicionesInvalidasYFechasAnteriores() {
        var b = new BloquePomodoro(1, TipoBloque.CONCENTRACION, Duration.ofSeconds(10), inicio);
        assertThrows(IllegalStateException.class, () -> b.reanudar(inicio.plusSeconds(1)));
        assertThrows(IllegalArgumentException.class, () -> b.pausar(inicio.minusSeconds(1)));
        b.pausar(inicio.plusSeconds(1));
        assertThrows(IllegalStateException.class, () -> b.pausar(inicio.plusSeconds(2)));
        b.cancelar(inicio.plusSeconds(3));
        assertEquals(EstadoBloque.CANCELADO, b.getEstado());
        assertEquals(Duration.ofSeconds(1), b.tiempoActivo(inicio.plusSeconds(100)));
    }

    @Test void cicloAlternaConcentracionYDescansoConDescansoLargoCadaCuatro() {
        var s = new SesionEstudio(new Estudiante(UUID.randomUUID(), "Estudiante"),
                new ConfiguracionPomodoro(Duration.ofSeconds(10), Duration.ofSeconds(2), Duration.ofSeconds(5), 4), inicio);
        Instant ahora = inicio;
        for (int i = 1; i <= 4; i++) {
            var foco = s.iniciarSiguienteBloque(ahora);
            assertEquals(TipoBloque.CONCENTRACION, foco.getTipo());
            ahora = ahora.plusSeconds(10);
            s.completarBloqueActual(ahora);
            assertTrue(s.tiempoRestanteBloqueActual(ahora).isEmpty());
            var descanso = s.iniciarSiguienteBloque(ahora);
            assertEquals(i == 4 ? TipoBloque.DESCANSO_LARGO : TipoBloque.DESCANSO_CORTO, descanso.getTipo());
            ahora = ahora.plus(descanso.getDuracionObjetivo());
            s.completarBloqueActual(ahora);
        }
        assertEquals(Duration.ofSeconds(40), s.tiempoEstudiado(ahora));
        assertEquals(8, s.getBloques().size());
        s.finalizar(ahora);
        assertEquals(EstadoSesion.FINALIZADA, s.getEstado());
        assertThrows(IllegalStateException.class, () -> s.iniciarSiguienteBloque(inicio.plusSeconds(1000)));
    }

    @Test void finalizarYAbandonarCierranBloqueSinAgregarTiempoDePausa() {
        var config = new ConfiguracionPomodoro(Duration.ofMinutes(25), Duration.ofMinutes(5), Duration.ofMinutes(15), 4);
        var s = new SesionEstudio(new Estudiante(UUID.randomUUID(), "Estudiante"), config, inicio);
        s.iniciarSiguienteBloque(inicio);
        assertThrows(IllegalStateException.class, () -> s.iniciarSiguienteBloque(inicio.plusSeconds(1)));
        s.pausar(inicio.plusSeconds(30));
        s.finalizar(inicio.plusSeconds(600));
        assertEquals(EstadoSesion.FINALIZADA, s.getEstado());
        assertEquals(EstadoBloque.CANCELADO, s.getBloques().get(0).getEstado());
        assertEquals(Duration.ofSeconds(30), s.tiempoEstudiado(inicio.plusSeconds(1000)));
        assertTrue(s.tiempoRestanteBloqueActual(inicio.plusSeconds(1000)).isEmpty());
        var otra = new SesionEstudio(s.getEstudiante(), config, inicio);
        otra.iniciarSiguienteBloque(inicio);
        otra.abandonar(inicio.plusSeconds(15));
        assertEquals(EstadoSesion.ABANDONADA, otra.getEstado());
        assertEquals(Duration.ofSeconds(15), otra.tiempoEstudiado(inicio.plusSeconds(1000)));
    }

    @Test void configuracionYNombreRechazanValoresInvalidos() {
        assertThrows(IllegalArgumentException.class, () -> new ConfiguracionPomodoro(Duration.ZERO, Duration.ofSeconds(1), Duration.ofSeconds(1), 4));
        assertThrows(IllegalArgumentException.class, () -> new ConfiguracionPomodoro(Duration.ofSeconds(1), Duration.ofSeconds(1), Duration.ofSeconds(1), 0));
        var estudiante = new Estudiante(UUID.randomUUID(), "Ana");
        estudiante.cambiarNombreVisible(" María ");
        assertEquals("María", estudiante.getNombreVisible());
        assertThrows(IllegalArgumentException.class, () -> estudiante.cambiarNombreVisible(" "));
    }
}
