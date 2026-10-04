package edu.proyecto;

import edu.proyecto.model.*;
import edu.proyecto.repository.*;
import edu.proyecto.service.ServicioPomodoro;
import jakarta.persistence.*;
import org.junit.jupiter.api.*;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class PomodoroPersistenciaTest {
    private EntityManagerFactory factory;
    private RepositorioSesionesJpa repo;
    private ServicioPomodoro servicio;
    private final Instant inicio = Instant.parse("2026-10-04T00:00:00Z");
    private final ConfiguracionPomodoro config = new ConfiguracionPomodoro(Duration.ofSeconds(10), Duration.ofSeconds(2), Duration.ofSeconds(5), 4);

    @BeforeEach void abrirBase() {
        factory = Persistence.createEntityManagerFactory("tarea3", Map.of(
                "jakarta.persistence.jdbc.url", "jdbc:h2:mem:test" + UUID.randomUUID(),
                "hibernate.hbm2ddl.auto", "create-drop", "hibernate.show_sql", "false"));
        repo = new RepositorioSesionesJpa(factory);
        servicio = new ServicioPomodoro(repo);
    }
    @AfterEach void cerrarBase() { if (factory != null) factory.close(); }

    @Test void guardaPausaYLaRecuperaConOtroRepositorio() {
        UUID estudiante = UUID.randomUUID();
        var sesion = servicio.iniciarSesion(estudiante, config, inicio);
        servicio.pausarTemporizador(sesion.getId(), inicio.plusSeconds(3));
        var otraInstancia = new ServicioPomodoro(new RepositorioSesionesJpa(factory));
        assertEquals(Duration.ofSeconds(7), otraInstancia.consultarTiempoRestante(sesion.getId(), inicio.plusSeconds(100)).orElseThrow());
        assertEquals(EstadoBloque.PAUSADO, repo.buscarPorId(sesion.getId()).orElseThrow().getBloques().get(0).getEstado());
        otraInstancia.reanudarTemporizador(sesion.getId(), inicio.plusSeconds(100));
        assertEquals(Duration.ofSeconds(6), servicio.consultarTiempoRestante(sesion.getId(), inicio.plusSeconds(101)).orElseThrow());
        assertEquals(1, repo.buscarPorEstudiante(estudiante).size());
    }

    @Test void persisteComposicionEstadosYOrdenDelHistorial() {
        UUID estudiante = UUID.randomUUID();
        var primera = servicio.iniciarSesion(estudiante, config, inicio);
        servicio.completarBloqueActual(primera.getId(), inicio.plusSeconds(10));
        servicio.iniciarSiguienteBloque(primera.getId(), inicio.plusSeconds(10));
        servicio.completarBloqueActual(primera.getId(), inicio.plusSeconds(12));
        servicio.finalizarSesion(primera.getId(), inicio.plusSeconds(12));
        var segunda = servicio.iniciarSesion(estudiante, config, inicio.plusSeconds(20));
        assertEquals(segunda.getId(), repo.buscarActivaPorEstudiante(estudiante).orElseThrow().getId());
        servicio.abandonarSesion(segunda.getId(), inicio.plusSeconds(23));
        var historial = servicio.consultarHistorial(estudiante);
        assertEquals(2, historial.size());
        assertEquals(segunda.getId(), historial.get(0).getId());
        assertEquals(EstadoSesion.ABANDONADA, historial.get(0).getEstado());
        assertEquals(EstadoSesion.FINALIZADA, historial.get(1).getEstado());
        assertEquals(2, historial.get(1).getBloques().size());
        assertEquals(TipoBloque.DESCANSO_CORTO, historial.get(1).getBloques().get(1).getTipo());
        assertEquals(Duration.ofSeconds(10), historial.get(1).tiempoEstudiado(inicio.plusSeconds(100)));
        assertTrue(repo.buscarActivaPorEstudiante(estudiante).isEmpty());
    }

    @Test void evitaDobleInicioYAislaHistorialDeEstudiantes() {
        UUID estudiante = UUID.randomUUID();
        var sesion = servicio.iniciarSesion(estudiante, config, inicio);
        assertThrows(IllegalStateException.class, () -> servicio.iniciarSesion(estudiante, config, inicio.plusSeconds(1)));
        assertEquals(1, repo.buscarPorEstudiante(estudiante).size());
        UUID otro = UUID.randomUUID();
        servicio.iniciarSesion(otro, config, inicio.plusSeconds(1));
        assertEquals(1, servicio.consultarHistorial(otro).size());
        assertEquals(sesion.getId(), servicio.consultarHistorial(estudiante).get(0).getId());
        assertTrue(repo.buscarPorId(UUID.randomUUID()).isEmpty());
        assertThrows(IllegalStateException.class, () -> servicio.reanudarTemporizador(sesion.getId(), inicio.plusSeconds(2)));
        assertEquals(EstadoBloque.EN_EJECUCION, repo.buscarPorId(sesion.getId()).orElseThrow().getBloques().get(0).getEstado());
    }

    @Test void entidadesRegistradasCoincidenConElDiagrama() {
        var tipos = factory.getMetamodel().getEntities().stream().map(e -> e.getJavaType().getSimpleName()).collect(java.util.stream.Collectors.toSet());
        assertEquals(Set.of("Estudiante", "SesionEstudio", "BloquePomodoro"), tipos);
        assertEquals(ConfiguracionPomodoro.class, factory.getMetamodel().embeddable(ConfiguracionPomodoro.class).getJavaType());
    }
}
