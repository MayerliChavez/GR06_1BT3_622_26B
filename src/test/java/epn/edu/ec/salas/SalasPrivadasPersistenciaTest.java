package epn.edu.ec.salas;

import epn.edu.ec.model.*;
import epn.edu.ec.repository.*;
import epn.edu.ec.service.*;
import jakarta.persistence.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/** Integración con JPA/H2; no se cuenta como prueba unitaria. */
class SalasPrivadasPersistenciaTest {
    private EntityManagerFactory factory;
    private RepositorioSalasPrivadasJpa salas;
    private RepositorioSesionesJpa sesiones;
    private RepositorioSesionesCompartidasJpa compartidas;
    private final ConfiguracionPomodoro configuracion = new ConfiguracionPomodoro(
            Duration.ofMinutes(25), Duration.ofMinutes(5), Duration.ofMinutes(15), 4);

    @BeforeEach void preparar() {
        factory = Persistence.createEntityManagerFactory("tarea3", Map.of(
                "jakarta.persistence.jdbc.url", "jdbc:h2:mem:salas" + UUID.randomUUID(),
                "hibernate.hbm2ddl.auto", "create-drop", "hibernate.show_sql", "false"));
        salas = new RepositorioSalasPrivadasJpa(factory);
        sesiones = new RepositorioSesionesJpa(factory);
        compartidas = new RepositorioSesionesCompartidasJpa(factory);
    }

    @AfterEach void cerrar() { if (factory != null) factory.close(); }

    @Test void recuperaSalaCodigoYResponsableConOtroRepositorio() {
        UUID ana = UUID.randomUUID();
        SalaPrivada creada = new ServicioSalasPrivadas(salas, sesiones, compartidas, () -> "K7M2QX").crear(ana);
        var otro = new RepositorioSalasPrivadasJpa(factory);
        SalaPrivada recuperada = otro.buscarAbiertaPorEstudiante(ana).orElseThrow();
        assertEquals(creada.getId(), recuperada.getId());
        assertEquals("K7M2QX", recuperada.getCodigo());
        assertEquals(ana, recuperada.getResponsableId());
        assertEquals(creada.getId(), otro.buscarAbiertaPorCodigo("K7M2QX").orElseThrow().getId());
        try (var em = factory.createEntityManager()) {
            assertEquals(1L, em.createQuery("select count(p) from ParticipacionSalaPrivada p", Long.class).getSingleResult());
        }
    }

    @Test void unaColisionEnBaseRevierteSalaYParticipacionJuntas() {
        UUID ana = UUID.randomUUID();
        salas.guardarNueva(new SalaPrivada(ana, "K7M2QX"));
        UUID luis = UUID.randomUUID();
        assertThrows(CodigoSalaOcupadoException.class,
                () -> salas.guardarNueva(new SalaPrivada(luis, "K7M2QX")));
        assertTrue(salas.buscarAbiertaPorEstudiante(luis).isEmpty());
        try (var em = factory.createEntityManager()) {
            assertEquals(1L, em.createQuery("select count(s) from SalaPrivada s", Long.class).getSingleResult());
            assertEquals(1L, em.createQuery("select count(p) from ParticipacionSalaPrivada p", Long.class).getSingleResult());
        }
    }

    @Test void salaSinCicloBloqueaInicioIndividualYEmparejado() {
        UUID ana = UUID.randomUUID();
        new ServicioSalasPrivadas(salas, sesiones, compartidas, () -> "K7M2QX").crear(ana);
        var individual = new ServicioPomodoro(sesiones, salas);
        var emparejado = new ServicioEstudioCompartido(new EmparejadorEnMemoria(), sesiones, compartidas, configuracion, salas);
        Instant ahora = Instant.parse("2026-11-07T15:00:00Z");
        assertThrows(IllegalStateException.class, () -> individual.iniciarSesion(ana, configuracion, ahora));
        assertThrows(IllegalStateException.class, () -> emparejado.solicitarEmparejamiento(ana, ahora));
        assertTrue(sesiones.buscarActivaPorEstudiante(ana).isEmpty());
        assertTrue(compartidas.buscarAbiertaPorEstudiante(ana).isEmpty());
    }

    @Test void carreraEntreCrearSalaEIniciarSesionDejaUnaSolaActividad() throws Exception {
        UUID ana = UUID.randomUUID();
        var privada = new ServicioSalasPrivadas(salas, sesiones, compartidas, () -> "K7M2QX");
        var individual = new ServicioPomodoro(sesiones, salas);
        var inicio = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> sala = executor.submit(() -> {
                inicio.await();
                try { privada.crear(ana); return true; }
                catch (IllegalStateException actividadOcupada) { return false; }
            });
            Future<Boolean> sesion = executor.submit(() -> {
                inicio.await();
                try { individual.iniciarSesion(ana, configuracion, Instant.now()); return true; }
                catch (IllegalStateException actividadOcupada) { return false; }
            });
            inicio.countDown();
            assertNotEquals(sala.get(15, TimeUnit.SECONDS), sesion.get(15, TimeUnit.SECONDS));
            assertNotEquals(salas.buscarAbiertaPorEstudiante(ana).isPresent(),
                    sesiones.buscarActivaPorEstudiante(ana).isPresent());
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(15, TimeUnit.SECONDS));
        }
    }
}
