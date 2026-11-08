package epn.edu.ec.salas;

import epn.edu.ec.model.*;
import epn.edu.ec.repository.*;
import epn.edu.ec.service.*;
import jakarta.persistence.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

class CicloSalaPrivadaPersistenciaTest {
    private EntityManagerFactory factory;
    private SalaPrivada sala;
    private ServicioCicloSalaPrivada ciclo;

    private final UUID ana = UUID.randomUUID();
    private final UUID luis = UUID.randomUUID();
    private final UUID pedro = UUID.randomUUID();

    private final Instant inicio =
            Instant.parse("2026-11-08T15:00:00Z");

    @BeforeEach
    void preparar() {
        factory = Persistence.createEntityManagerFactory(
                "tarea3",
                Map.of(
                        "jakarta.persistence.jdbc.url",
                        "jdbc:h2:mem:cicloprivado"
                                + UUID.randomUUID()
                                + ";LOCK_TIMEOUT=10000",
                        "hibernate.hbm2ddl.auto", "create-drop",
                        "hibernate.show_sql", "false"
                )
        );

        try (var em = factory.createEntityManager()) {
            em.getTransaction().begin();
            em.persist(new Estudiante(ana, "Ana"));
            em.persist(new Estudiante(luis, "Luis"));
            em.persist(new Estudiante(pedro, "Pedro"));
            em.getTransaction().commit();
        }

        sala = new SalaPrivada(ana, "K7M2QX");

        new RepositorioSalasPrivadasJpa(factory)
                .guardarNueva(sala);

        ciclo = new ServicioCicloSalaPrivada(factory);
    }

    @AfterEach
    void cerrar() {
        if (factory != null) {
            factory.close();
        }
    }

    private void agregarLuis() {
        try (var em = factory.createEntityManager()) {
            em.getTransaction().begin();

            em.persist(new ParticipacionSalaPrivada(
                    luis,
                    em.getReference(SalaPrivada.class, sala.getId())
            ));

            em.getTransaction().commit();
        }
    }

    @Test
    void soloResponsableConAlMenosDosParticipantesPuedeIniciar() {
        assertThrows(
                IllegalStateException.class,
                () -> ciclo.iniciar(ana, inicio)
        );

        agregarLuis();

        assertThrows(
                SecurityException.class,
                () -> ciclo.iniciar(luis, inicio)
        );

        assertTrue(
                new RepositorioSesionesJpa(factory)
                        .buscarPorEstudiante(ana)
                        .isEmpty()
        );
    }

    @Test
    void inicioComunYRecargaConservanBloqueVencimientoYSesiones() {
        agregarLuis();
        ciclo.iniciar(ana, inicio);

        var estadoAna =
                ciclo.consultar(ana, inicio.plusSeconds(60));

        var estadoLuis =
                new ServicioCicloSalaPrivada(factory)
                        .consultar(luis, inicio.plusSeconds(60));

        assertEquals(
                estadoAna.finBloque(),
                estadoLuis.finBloque()
        );
        assertEquals(1440000L, estadoAna.restanteMillis());

        var repo = new RepositorioSesionesJpa(factory);

        assertEquals(
                inicio,
                repo.buscarActivaPorEstudiante(ana)
                        .orElseThrow()
                        .getFechaInicio()
        );
        assertEquals(
                inicio,
                repo.buscarActivaPorEstudiante(luis)
                        .orElseThrow()
                        .getFechaInicio()
        );
        assertEquals(1, repo.buscarPorEstudiante(ana).size());
    }

    @Test
    void avanceDelServidorRecuperaVencimientosSinContarDescansos() {
        agregarLuis();
        ciclo.iniciar(ana, inicio);

        // Cuatro concentraciones y tres descansos cortos:
        // 4 * 25 + 3 * 5 = 115 minutos.
        Instant finCuarta = inicio.plusSeconds(115 * 60);

        ciclo.avanzarAbiertas(finCuarta);

        assertEquals(
                Duration.ofMinutes(100),
                new RepositorioSesionesJpa(factory)
                        .buscarActivaPorEstudiante(luis)
                        .orElseThrow()
                        .tiempoEstudiado(finCuarta)
        );

        var estado = ciclo.consultar(luis, finCuarta);

        assertEquals(TipoBloque.DESCANSO_LARGO, estado.tipo());
        assertEquals(900000L, estado.restanteMillis());
    }

    @Test
    void dosRevisionesConcurrentesNoCreanBloquesDuplicados()
            throws Exception {

        agregarLuis();
        ciclo.iniciar(ana, inicio);

        var executor = Executors.newFixedThreadPool(2);
        var empezar = new CountDownLatch(1);

        try {
            var a = executor.submit(() -> {
                empezar.await();
                ciclo.avanzar(
                        sala.getId(),
                        inicio.plusSeconds(1500)
                );
                return true;
            });

            var b = executor.submit(() -> {
                empezar.await();
                ciclo.avanzar(
                        sala.getId(),
                        inicio.plusSeconds(1500)
                );
                return true;
            });

            empezar.countDown();

            assertTrue(a.get(20, TimeUnit.SECONDS));
            assertTrue(b.get(20, TimeUnit.SECONDS));

            assertEquals(
                    2,
                    new RepositorioSesionesJpa(factory)
                            .buscarActivaPorEstudiante(ana)
                            .orElseThrow()
                            .getBloques()
                            .size()
            );
        } finally {
            executor.shutdownNow();

            assertTrue(
                    executor.awaitTermination(
                            20,
                            TimeUnit.SECONDS
                    )
            );
        }
    }

    @Test
    void incorporacionDuranteDescansoNoAtribuyeConcentracionAnterior() {
        agregarLuis();

        Instant inicioReal = Instant.now()
                .minusSeconds(1510)
                .truncatedTo(ChronoUnit.MILLIS);

        ciclo.iniciar(ana, inicioReal);

        var solicitudes =
                new RepositorioSolicitudesSalaJpa(factory);

        var solicitud =
                new SolicitudIngresoSala(pedro, sala, Instant.now());

        solicitudes.guardar(solicitud);

        var sesiones = new RepositorioSesionesJpa(factory);

        var ingreso = new ServicioIngresoSala(
                solicitudes,
                new RepositorioSalasPrivadasJpa(factory),
                sesiones,
                new RepositorioSesionesCompartidasJpa(factory)
        );

        ingreso.aceptar(ana, solicitud.getId());

        var personal =
                sesiones.buscarActivaPorEstudiante(pedro)
                        .orElseThrow();

        Instant finDescanso = inicioReal.plusSeconds(1800);

        assertEquals(
                TipoBloque.DESCANSO_CORTO,
                personal.getBloques().get(0).getTipo()
        );
        assertEquals(
                Duration.ZERO,
                personal.tiempoEstudiado(finDescanso)
        );
        assertTrue(
                personal.getFechaInicio()
                        .isAfter(inicioReal.plusSeconds(1500))
        );

        ciclo.avanzar(sala.getId(), finDescanso);

        assertEquals(
                TipoBloque.CONCENTRACION,
                ciclo.consultar(pedro, finDescanso).tipo()
        );
        assertEquals(
                2,
                sesiones.buscarActivaPorEstudiante(pedro)
                        .orElseThrow()
                        .getBloques()
                        .size()
        );
    }

    @Test
    void sesionPrivadaNoPuedePausarseDesdeServicioIndividual() {
        agregarLuis();
        ciclo.iniciar(ana, inicio);

        var sesiones = new RepositorioSesionesJpa(factory);

        var personal =
                sesiones.buscarActivaPorEstudiante(luis)
                        .orElseThrow();

        var individual = new ServicioPomodoro(
                sesiones,
                new RepositorioSalasPrivadasJpa(factory)
        );

        assertThrows(
                IllegalStateException.class,
                () -> individual.pausarTemporizador(
                        personal.getId(),
                        inicio.plusSeconds(30)
                )
        );

        assertEquals(
                EstadoBloque.EN_EJECUCION,
                sesiones.buscarPorId(personal.getId())
                        .orElseThrow()
                        .getBloques().get(0)
                        .getEstado()
        );

        assertEquals(
                Duration.ofSeconds(1470),
                individual.consultarTiempoRestante(
                        personal.getId(),
                        inicio.plusSeconds(30)
                ).orElseThrow()
        );
    }
}