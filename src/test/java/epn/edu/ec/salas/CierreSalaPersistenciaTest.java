package epn.edu.ec.salas;

import epn.edu.ec.model.*;
import epn.edu.ec.repository.*;
import epn.edu.ec.service.*;
import jakarta.persistence.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

class CierreSalaPersistenciaTest {

    private EntityManagerFactory factory;
    private ServicioCierreSalaPrivada cierre;
    private RepositorioSesionesJpa sesiones;
    private RepositorioSalasPrivadasJpa salas;
    private SalaPrivada sala;

    private final UUID ana = UUID.randomUUID();
    private final UUID luis = UUID.randomUUID();

    private final Instant inicio =
            Instant.parse("2026-11-08T15:00:00Z");

    @BeforeEach
    void preparar() {
        factory = Persistence.createEntityManagerFactory(
                "tarea3",
                Map.of(
                        "jakarta.persistence.jdbc.url",
                        "jdbc:h2:mem:cierre"
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
            em.getTransaction().commit();
        }

        sesiones = new RepositorioSesionesJpa(factory);
        salas = new RepositorioSalasPrivadasJpa(factory);
        cierre = new ServicioCierreSalaPrivada(factory);

        sala = new SalaPrivada(ana, "K7M2QX");
        salas.guardarNueva(sala);
    }

    @AfterEach
    void cerrarFactory() {
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

    private void iniciarConDos() {
        agregarLuis();

        new ServicioCicloSalaPrivada(factory)
                .iniciar(ana, inicio);
    }

    private SalaPrivada salaPersistida() {
        try (var em = factory.createEntityManager()) {
            return em.find(SalaPrivada.class, sala.getId());
        }
    }

    @Test
    void cerrarSinCompanerosNoCreaUnaSesionDeEstudio() {
        assertTrue(
                cierre.cerrar(ana, sala.getId(), true, inicio)
        );

        assertEquals(
                SalaPrivada.Estado.CERRADA,
                salaPersistida().getEstado()
        );
        assertTrue(sesiones.buscarPorEstudiante(ana).isEmpty());
        assertTrue(salas.buscarAbiertaPorEstudiante(ana).isEmpty());
    }

    @Test
    void cerrarConCompanerosAntesDeIniciarNoRegistraTiempo() {
        agregarLuis();

        assertTrue(
                cierre.cerrar(ana, sala.getId(), true, inicio)
        );

        assertTrue(sesiones.buscarPorEstudiante(ana).isEmpty());
        assertTrue(sesiones.buscarPorEstudiante(luis).isEmpty());
        assertTrue(salas.buscarAbiertaPorEstudiante(luis).isEmpty());
    }

    @Test
    void cerrarEnConcentracionConserva18MinutosParaAmbos() {
        iniciarConDos();
        Instant fin = inicio.plusSeconds(18 * 60);

        assertTrue(
                cierre.cerrar(ana, sala.getId(), true, fin)
        );

        for (UUID id : List.of(ana, luis)) {
            var historial = sesiones.buscarPorEstudiante(id);

            assertEquals(1, historial.size());
            assertEquals(
                    EstadoSesion.FINALIZADA,
                    historial.get(0).getEstado()
            );
            assertEquals(
                    Duration.ofMinutes(18),
                    historial.get(0).tiempoEstudiado(fin)
            );
            assertEquals(fin, historial.get(0).getFechaFin());
        }

        assertFalse(
                cierre.cerrar(
                        ana,
                        sala.getId(),
                        true,
                        fin.plusSeconds(60)
                )
        );

        assertEquals(fin, salaPersistida().getFechaCierre());
        assertEquals(1, sesiones.buscarPorEstudiante(ana).size());
    }

    @Test
    void cerrarDuranteDescansoNoCuentaElDescansoComoEstudio() {
        iniciarConDos();
        Instant fin = inicio.plusSeconds(26 * 60);

        assertTrue(
                cierre.cerrar(ana, sala.getId(), true, fin)
        );

        for (UUID id : List.of(ana, luis)) {
            var personal = sesiones.buscarPorEstudiante(id).get(0);

            assertEquals(
                    Duration.ofMinutes(25),
                    personal.tiempoEstudiado(fin)
            );
            assertEquals(EstadoSesion.FINALIZADA, personal.getEstado());
        }
    }

    @Test
    void cancelarLaConfirmacionMantieneElCicloEnEjecucion() {
        iniciarConDos();
        Instant ahora = inicio.plusSeconds(18 * 60);

        assertFalse(
                cierre.cerrar(ana, sala.getId(), false, ahora)
        );

        assertEquals(
                SalaPrivada.Estado.ABIERTA,
                salaPersistida().getEstado()
        );
        assertTrue(sesiones.buscarActivaPorEstudiante(ana).isPresent());
        assertTrue(cierre.consumirAviso(luis).isEmpty());

        assertEquals(
                Duration.ofMinutes(18),
                sesiones.buscarActivaPorEstudiante(luis)
                        .orElseThrow()
                        .tiempoEstudiado(ahora)
        );
    }

    @Test
    void participanteNoPuedeCerrarLaSala() {
        iniciarConDos();

        assertThrows(
                SecurityException.class,
                () -> cierre.cerrar(
                        luis,
                        sala.getId(),
                        true,
                        inicio.plusSeconds(60)
                )
        );

        assertEquals(
                SalaPrivada.Estado.ABIERTA,
                salaPersistida().getEstado()
        );
        assertTrue(sesiones.buscarActivaPorEstudiante(ana).isPresent());
    }

    @Test
    void avisoSeRecuperaDesdeOtraInstanciaYSeConsumeUnaVez() {
        iniciarConDos();

        assertTrue(
                cierre.cerrar(
                        ana,
                        sala.getId(),
                        true,
                        inicio.plusSeconds(18 * 60)
                )
        );

        var otraInstancia = new ServicioCierreSalaPrivada(factory);

        assertEquals(
                "El responsable cerró la sala. Tu tiempo de estudio quedó guardado.",
                otraInstancia.consumirAviso(luis).orElseThrow()
        );
        assertTrue(otraInstancia.consumirAviso(luis).isEmpty());

        assertEquals(
                "La sala se cerró correctamente.",
                otraInstancia.consumirAviso(ana).orElseThrow()
        );

        var nueva = new ServicioSalasPrivadas(
                salas,
                sesiones,
                new RepositorioSesionesCompartidasJpa(factory),
                () -> "R8T4YX"
        ).crear(ana);

        assertNotEquals(sala.getId(), nueva.getId());
    }

    @Test
    void falloAlGuardarAvisosRevierteTodoElCierre() {
        iniciarConDos();

        try (var em = factory.createEntityManager()) {
            em.getTransaction().begin();

            em.createNativeQuery(
                    "alter table aviso_cierre_sala_privada "
                            + "add constraint prueba_aviso_cierre "
                            + "check (mensaje <> 'La sala se cerró correctamente.')"
            ).executeUpdate();

            em.getTransaction().commit();
        }

        assertThrows(
                RuntimeException.class,
                () -> cierre.cerrar(
                        ana,
                        sala.getId(),
                        true,
                        inicio.plusSeconds(18 * 60)
                )
        );

        assertEquals(
                SalaPrivada.Estado.ABIERTA,
                salaPersistida().getEstado()
        );
        assertTrue(sesiones.buscarActivaPorEstudiante(ana).isPresent());
        assertTrue(sesiones.buscarActivaPorEstudiante(luis).isPresent());
        assertTrue(salas.buscarAbiertaPorEstudiante(luis).isPresent());
        assertTrue(cierre.consumirAviso(ana).isEmpty());
    }
}