package epn.edu.ec.salas;

import epn.edu.ec.model.*;
import epn.edu.ec.repository.*;
import epn.edu.ec.service.*;
import jakarta.persistence.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

class IngresoSalaPersistenciaTest {

    private EntityManagerFactory factory;
    private RepositorioSolicitudesSalaJpa solicitudes;
    private RepositorioSesionesJpa sesiones;
    private ServicioIngresoSala ingreso;
    private SalaPrivada sala;

    private final UUID ana = UUID.randomUUID();
    private final UUID luis = UUID.randomUUID();

    private final Instant ahora =
            Instant.parse("2026-11-07T15:00:00Z");

    @BeforeEach
    void preparar() {
        factory = Persistence.createEntityManagerFactory(
                "tarea3",
                Map.of(
                        "jakarta.persistence.jdbc.url",
                        "jdbc:h2:mem:ingreso" + UUID.randomUUID(),
                        "hibernate.hbm2ddl.auto", "create-drop",
                        "hibernate.show_sql", "false"
                )
        );

        solicitudes = new RepositorioSolicitudesSalaJpa(factory);
        sesiones = new RepositorioSesionesJpa(factory);

        var salas = new RepositorioSalasPrivadasJpa(factory);

        sala = new SalaPrivada(ana, "K7M2QX");
        salas.guardarNueva(sala);

        ingreso = new ServicioIngresoSala(
                solicitudes,
                salas,
                sesiones,
                new RepositorioSesionesCompartidasJpa(factory)
        );
    }

    @AfterEach
    void cerrar() {
        if (factory != null) {
            factory.close();
        }
    }

    @Test
    void repositorioRecuperaSolicitudYSalaConOtraInstancia() {
        var creada = new SolicitudIngresoSala(luis, sala, ahora);

        solicitudes.guardar(creada);

        var recuperada = new RepositorioSolicitudesSalaJpa(factory)
                .buscarPorId(creada.getId())
                .orElseThrow();

        assertEquals(luis, recuperada.getEstudianteId());
        assertEquals(sala.getId(), recuperada.getSala().getId());
        assertEquals(
                SolicitudIngresoSala.Estado.PENDIENTE,
                recuperada.getEstado()
        );
    }

    @Test
    void solicitarNormalizaCodigoYNoIncorporaAlEstudiante() {
        var creada = ingreso.solicitar(luis, " k7m2qx ", ahora);

        assertEquals(
                creada.getId(),
                ingreso.consultarPendiente(luis).orElseThrow().getId()
        );

        assertEquals(
                SolicitudIngresoSala.Estado.PENDIENTE,
                creada.getEstado()
        );

        assertEquals(
                1L,
                solicitudes.contarParticipantes(sala.getId())
        );

        assertEquals(
                1,
                ingreso.pendientesParaResponsable(ana).size()
        );

        assertThrows(
                IllegalStateException.class,
                () -> ingreso.pendientesParaResponsable(luis)
        );
    }

    @Test
    void unaPendienteSeConservaYDebeCancelarseAntesDeOtraSala() {
        var primera = ingreso.solicitar(luis, "K7M2QX", ahora);

        assertEquals(
                primera.getId(),
                ingreso.solicitar(luis, "K7M2QX", ahora).getId()
        );

        var otraSala =
                new SalaPrivada(UUID.randomUUID(), "P4R8TW");

        new RepositorioSalasPrivadasJpa(factory)
                .guardarNueva(otraSala);

        var error = assertThrows(
                IllegalStateException.class,
                () -> ingreso.solicitar(luis, "P4R8TW", ahora)
        );

        assertEquals(
                "Cancela tu solicitud pendiente antes de solicitar "
                        + "el ingreso a otra sala.",
                error.getMessage()
        );

        ingreso.cancelar(luis);

        assertTrue(ingreso.consultarPendiente(luis).isEmpty());
        assertTrue(ingreso.pendientesParaResponsable(ana).isEmpty());

        assertEquals(
                SolicitudIngresoSala.Estado.CANCELADA,
                solicitudes.buscarPorId(primera.getId())
                        .orElseThrow()
                        .getEstado()
        );

        assertEquals(
                otraSala.getId(),
                ingreso.solicitar(luis, "P4R8TW", ahora)
                        .getSala()
                        .getId()
        );
    }

    @Test
    void codigoInexistenteNoGuardaSolicitud() {
        var error = assertThrows(
                IllegalArgumentException.class,
                () -> ingreso.solicitar(luis, "ZZ99AA", ahora)
        );

        assertEquals(
                "El código ingresado no corresponde a ninguna sala abierta.",
                error.getMessage()
        );

        assertTrue(
                solicitudes.buscarPendientePorEstudiante(luis).isEmpty()
        );
    }

    @Test
    void actividadIndividualImpideSolicitarIngreso() {
        var configuracion = new ConfiguracionPomodoro(
                Duration.ofMinutes(25),
                Duration.ofMinutes(5),
                Duration.ofMinutes(15),
                4
        );

        new ServicioPomodoro(sesiones)
                .iniciarSesion(luis, configuracion, ahora);

        assertThrows(
                IllegalStateException.class,
                () -> ingreso.solicitar(luis, "K7M2QX", ahora)
        );

        assertTrue(
                solicitudes.buscarPendientePorEstudiante(luis).isEmpty()
        );
    }

    @Test
    void salaLlenaImpideCrearSolicitud() {
        try (var em = factory.createEntityManager()) {
            var tx = em.getTransaction();
            tx.begin();

            var referencia =
                    em.getReference(SalaPrivada.class, sala.getId());

            for (int i = 0; i < 3; i++) {
                em.persist(
                        new ParticipacionSalaPrivada(
                                UUID.randomUUID(),
                                referencia
                        )
                );
            }

            tx.commit();
        }

        var error = assertThrows(
                IllegalStateException.class,
                () -> ingreso.solicitar(luis, "K7M2QX", ahora)
        );

        assertEquals(
                "La sala está llena. No es posible enviar la solicitud.",
                error.getMessage()
        );

        assertTrue(
                solicitudes.buscarPendientePorEstudiante(luis).isEmpty()
        );
    }
}