package epn.edu.ec.salas;

import epn.edu.ec.model.*;
import epn.edu.ec.repository.*;
import epn.edu.ec.service.*;
import jakarta.persistence.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.*;
import static org.junit.Assert.*;

/** Verifica transacciones y carreras reales; no se cuenta como prueba unitaria. */
public class DecisionSalaPersistenciaTest extends PruebaSalaPrivada {
    private EntityManagerFactory factory;
    private RepositorioSolicitudesSalaJpa solicitudes;
    private RepositorioSesionesJpa sesiones;
    private RepositorioSalasPrivadasJpa salas;
    private ServicioIngresoSala servicio;
    private SalaPrivada sala;
    private final UUID ana = UUID.randomUUID(), luis = UUID.randomUUID(), pedro = UUID.randomUUID(),
            sofia = UUID.randomUUID(), carla = UUID.randomUUID();

    @Before
    public void setUp() {
        factory = Persistence.createEntityManagerFactory("tarea3", Map.of(
                "jakarta.persistence.jdbc.url", "jdbc:h2:mem:decision" + UUID.randomUUID() + ";LOCK_TIMEOUT=10000",
                "hibernate.hbm2ddl.auto", "create-drop", "hibernate.show_sql", "false"));
        try (var em = factory.createEntityManager()) {
            em.getTransaction().begin();
            for (UUID id : List.of(ana, luis, pedro, sofia, carla)) em.persist(new Estudiante(id, "Estudiante"));
            em.getTransaction().commit();
        }
        sesiones = new RepositorioSesionesJpa(factory);
        salas = new RepositorioSalasPrivadasJpa(factory);
        solicitudes = new RepositorioSolicitudesSalaJpa(factory);
        servicio = new ServicioIngresoSala(solicitudes, salas, sesiones, new RepositorioSesionesCompartidasJpa(factory));
        sala = new SalaPrivada(ana, "K7M2QX");
        salas.guardarNueva(sala);
    }

    @After
    public void tearDown() {
        if (factory != null) {
            factory.close();
        }
    }

    private SolicitudIngresoSala solicitar(UUID estudiante) {
        var solicitud = new SolicitudIngresoSala(estudiante, sala, Instant.now());
        solicitudes.guardar(solicitud);
        return solicitud;
    }

    private void fase(TipoBloque bloque) {
        try (var em = factory.createEntityManager()) {
            em.getTransaction().begin();
            em.find(SalaPrivada.class, sala.getId()).actualizarBloqueActual(bloque);
            em.getTransaction().commit();
        }
    }

    @Test
    public void given_solicitud_pendiente_when_aceptar_then_guarda_decision_y_participante_juntos() {
        var solicitud = solicitar(luis);
        servicio.aceptar(ana, solicitud.getId());
        assertEquals(SolicitudIngresoSala.Estado.ACEPTADA, solicitudes.buscarPorId(solicitud.getId()).orElseThrow().getEstado());
        assertEquals(sala.getId(), salas.buscarAbiertaPorEstudiante(luis).orElseThrow().getId());
        assertEquals(2, solicitudes.contarParticipantes(sala.getId()));
        assertTrue(servicio.consultarPendiente(luis).isEmpty());
        assertThrows(IllegalStateException.class, () -> servicio.aceptar(ana, solicitud.getId()));
        assertEquals(2, solicitudes.contarParticipantes(sala.getId()));
    }

    @Test
    public void given_participante_when_consultar_y_gestionar_espera_then_solo_lee_pendientes_de_su_sala() {
        servicio.aceptar(ana, solicitar(luis).getId());
        var solicitudPedro = solicitar(pedro);
        var solicitudSofia = solicitar(sofia);
        assertEquals(2, servicio.pendientesParaParticipante(luis).size());
        assertThrows(SecurityException.class, () -> servicio.aceptar(luis, solicitudPedro.getId()));
        assertThrows(SecurityException.class, () -> servicio.rechazar(luis, solicitudPedro.getId()));
        assertThrows(SecurityException.class, () -> servicio.pendientesParaParticipante(carla));

        salas.guardarNueva(new SalaPrivada(carla, "P4R8TW"));
        assertTrue(servicio.pendientesParaParticipante(carla).isEmpty());
        servicio.cancelar(sofia);
        assertEquals(List.of(solicitudPedro.getId()), servicio.pendientesParaParticipante(luis)
                .stream().map(SolicitudIngresoSala::getId).toList());
        assertEquals(SolicitudIngresoSala.Estado.CANCELADA,
                solicitudes.buscarPorId(solicitudSofia.getId()).orElseThrow().getEstado());
    }

    @Test
    public void given_estudiante_ajeno_when_aceptar_o_rechazar_then_solicitud_permanece_pendiente() {
        var solicitud = solicitar(luis);
        assertThrows(SecurityException.class, () -> servicio.aceptar(pedro, solicitud.getId()));
        assertThrows(SecurityException.class, () -> servicio.rechazar(pedro, solicitud.getId()));
        assertEquals(SolicitudIngresoSala.Estado.PENDIENTE, solicitudes.buscarPorId(solicitud.getId()).orElseThrow().getEstado());
        assertEquals(1, solicitudes.contarParticipantes(sala.getId()));
    }

    @Test
    public void given_solicitud_pendiente_when_aceptar_en_concentracion_y_descanso_then_solo_acepta_en_descanso() {
        var solicitud = solicitar(luis);
        fase(TipoBloque.CONCENTRACION);
        var error = assertThrows(IllegalStateException.class, () -> servicio.aceptar(ana, solicitud.getId()));
        assertEquals("Podrás aceptar nuevos participantes en el siguiente descanso.", error.getMessage());
        assertEquals(SolicitudIngresoSala.Estado.PENDIENTE, solicitudes.buscarPorId(solicitud.getId()).orElseThrow().getEstado());
        fase(TipoBloque.DESCANSO_CORTO);
        servicio.aceptar(ana, solicitud.getId());
        assertTrue(salas.buscarAbiertaPorEstudiante(luis).isPresent());
    }

    @Test
    public void given_solicitud_en_concentracion_when_rechazar_then_rechazo_inmediato_sin_ingreso() {
        var solicitud = solicitar(luis);
        fase(TipoBloque.CONCENTRACION);
        servicio.rechazar(ana, solicitud.getId());
        assertEquals(SolicitudIngresoSala.Estado.RECHAZADA, solicitudes.buscarPorId(solicitud.getId()).orElseThrow().getEstado());
        assertTrue(servicio.consultarPendiente(luis).isEmpty());
        assertTrue(salas.buscarAbiertaPorEstudiante(luis).isEmpty());
    }

    @Test
    public void given_solicitante_con_actividad_iniciada_when_aceptar_then_rechaza_y_conserva_pendiente() {
        var solicitud = solicitar(luis);
        new ServicioPomodoro(sesiones, salas).iniciarSesion(luis,
                new ConfiguracionPomodoro(Duration.ofMinutes(25), Duration.ofMinutes(5), Duration.ofMinutes(15), 4), Instant.now());
        assertThrows(IllegalStateException.class, () -> servicio.aceptar(ana, solicitud.getId()));
        assertEquals(SolicitudIngresoSala.Estado.PENDIENTE, solicitudes.buscarPorId(solicitud.getId()).orElseThrow().getEstado());
        assertTrue(salas.buscarAbiertaPorEstudiante(luis).isEmpty());
    }

    @Test
    public void given_ultimo_cupo_when_aceptar_dos_solicitudes_concurrentes_then_solo_una_aceptada() throws Exception {
        try (var em = factory.createEntityManager()) {
            em.getTransaction().begin();
            var referencia = em.getReference(SalaPrivada.class, sala.getId());
            em.persist(new ParticipacionSalaPrivada(sofia, referencia));
            em.persist(new ParticipacionSalaPrivada(carla, referencia));
            em.getTransaction().commit();
        }
        var primera = solicitar(luis);
        var segunda = solicitar(pedro);
        var inicio = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        try {
            var a = executor.submit(() -> decidirTras(inicio, primera.getId()));
            var b = executor.submit(() -> decidirTras(inicio, segunda.getId()));
            inicio.countDown();
            assertNotEquals(a.get(20, TimeUnit.SECONDS), b.get(20, TimeUnit.SECONDS));
            assertEquals(4, solicitudes.contarParticipantes(sala.getId()));
            assertEquals(1, solicitudes.pendientesDeSala(sala.getId()).size());
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(20, TimeUnit.SECONDS));
        }
    }

    private boolean decidirTras(CountDownLatch inicio, UUID solicitudId) throws Exception {
        inicio.await();
        try { solicitudes.decidir(ana, solicitudId, true); return true; }
        catch (IllegalStateException salaLlena) { return false; }
    }

    @Test
    public void given_solicitud_pendiente_when_aceptar_y_cancelar_concurrentemente_then_una_sola_decision_consistente() throws Exception {
        var solicitud = solicitar(luis);
        var inicio = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        try {
            var a = executor.submit(() -> decidirTras(inicio, solicitud.getId()));
            var b = executor.submit(() -> {
                inicio.await();
                try { solicitudes.cancelarPendiente(luis); return true; }
                catch (IllegalStateException yaAtendida) { return false; }
            });
            inicio.countDown();
            assertNotEquals(a.get(20, TimeUnit.SECONDS), b.get(20, TimeUnit.SECONDS));
            var estado = solicitudes.buscarPorId(solicitud.getId()).orElseThrow().getEstado();
            assertTrue(estado == SolicitudIngresoSala.Estado.ACEPTADA || estado == SolicitudIngresoSala.Estado.CANCELADA);
            assertEquals(estado == SolicitudIngresoSala.Estado.ACEPTADA, salas.buscarAbiertaPorEstudiante(luis).isPresent());
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(20, TimeUnit.SECONDS));
        }
    }

    @Test
    public void given_fallo_de_base_when_aceptar_then_revierte_decision_y_participacion() {
        var solicitud = solicitar(luis);
        try (var em = factory.createEntityManager()) {
            em.getTransaction().begin();
            em.createNativeQuery("alter table solicitud_ingreso_sala add constraint prueba_rollback check (estado <> 'ACEPTADA')").executeUpdate();
            em.getTransaction().commit();
        }
        assertThrows(RuntimeException.class, () -> servicio.aceptar(ana, solicitud.getId()));
        assertEquals(SolicitudIngresoSala.Estado.PENDIENTE, solicitudes.buscarPorId(solicitud.getId()).orElseThrow().getEstado());
        assertTrue(salas.buscarAbiertaPorEstudiante(luis).isEmpty());
        assertEquals(1, solicitudes.contarParticipantes(sala.getId()));
    }
}
