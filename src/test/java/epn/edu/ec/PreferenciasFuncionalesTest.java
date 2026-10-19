package epn.edu.ec;

import epn.edu.ec.model.*;
import epn.edu.ec.repository.*;
import epn.edu.ec.service.ServicioPomodoro;
import epn.edu.ec.tdd.preferencias.ServicioPreferencias;
import jakarta.persistence.*;
import org.junit.jupiter.api.*;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class PreferenciasFuncionalesTest {
    private EntityManagerFactory factory;
    private ServicioPreferencias servicio;
    private RepositorioSesionesJpa sesiones;
    private final Instant inicio = Instant.parse("2026-10-18T15:00:00Z");

    @BeforeEach void preparar() {
        factory = Persistence.createEntityManagerFactory("tarea3", Map.of(
                "jakarta.persistence.jdbc.url", "jdbc:h2:mem:preferencias" + UUID.randomUUID(),
                "hibernate.hbm2ddl.auto", "create-drop", "hibernate.show_sql", "false"));
        sesiones = new RepositorioSesionesJpa(factory);
        servicio = new ServicioPreferencias(new RepositorioPreferenciasJpa(factory), new ServicioPomodoro(sesiones));
    }
    @AfterEach void cerrar() { factory.close(); }

    @Test void persistePorEstudianteYRecuperaConOtroRepositorio() {
        UUID id = UUID.randomUUID();
        servicio.guardar(id, 40, 8, 20, 3);
        var recuperada = new RepositorioPreferenciasJpa(factory).buscarPorEstudiante(id);
        assertEquals(Duration.ofMinutes(40), recuperada.duracionPara(TipoBloque.CONCENTRACION));
        assertEquals(Duration.ofMinutes(8), recuperada.duracionPara(TipoBloque.DESCANSO_CORTO));
        assertEquals(Duration.ofMinutes(20), recuperada.duracionPara(TipoBloque.DESCANSO_LARGO));
        assertEquals(3, recuperada.getBloquesAntesDescansoLargo());
        assertEquals(Duration.ofMinutes(25), servicio.consultar(UUID.randomUUID()).duracionPara(TipoBloque.CONCENTRACION));
    }

    @Test void cambiarPreferenciasConservaTodaLaSesionActivaYAplicaEnLaSiguiente() {
        UUID id = UUID.randomUUID();
        servicio.guardar(id, 1, 2, 3, 1);
        var anterior = servicio.iniciarSesion(id, inicio);
        servicio.guardar(id, 40, 8, 20, 3);
        var recuperada = sesiones.buscarPorId(anterior.getId()).orElseThrow();
        assertEquals(Duration.ofMinutes(1), recuperada.tiempoRestanteBloqueActual(inicio).orElseThrow());
        recuperada.completarBloqueActual(inicio.plusSeconds(60));
        var descanso = recuperada.iniciarSiguienteBloque(inicio.plusSeconds(60));
        assertEquals(TipoBloque.DESCANSO_LARGO, descanso.getTipo());
        assertEquals(Duration.ofMinutes(3), descanso.getDuracionObjetivo());
        var pomodoro = new ServicioPomodoro(sesiones);
        pomodoro.finalizarSesion(anterior.getId(), inicio.plusSeconds(61));
        var nueva = servicio.iniciarSesion(id, inicio.plusSeconds(62));
        assertEquals(Duration.ofMinutes(40), nueva.tiempoRestanteBloqueActual(inicio.plusSeconds(62)).orElseThrow());
        assertEquals(3, nueva.getConfiguracion().getBloquesAntesDescansoLargo());
    }

    @Test void valoresInvalidosNoReemplazanPreferenciasGuardadas() {
        UUID id = UUID.randomUUID();
        servicio.guardar(id, 30, 5, 15, 4);
        for (int[] caso : new int[][]{{0,5,15,4},{30,0,15,4},{30,5,-1,4},{30,5,15,0}}) {
            assertThrows(IllegalArgumentException.class, () -> servicio.guardar(id, caso[0], caso[1], caso[2], caso[3]));
        }
        assertEquals(Duration.ofMinutes(30), servicio.consultar(id).duracionPara(TipoBloque.CONCENTRACION));
    }
}
