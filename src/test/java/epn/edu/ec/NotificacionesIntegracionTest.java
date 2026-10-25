package epn.edu.ec;

import epn.edu.ec.model.*;
import epn.edu.ec.repository.*;
import epn.edu.ec.service.*;
import epn.edu.ec.tdd.notificaciones.*;
import jakarta.persistence.*;
import org.junit.jupiter.api.*;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class NotificacionesIntegracionTest {
    private EntityManagerFactory factory;
    private RepositorioSesionesJpa individuales;
    private RepositorioSesionesCompartidasJpa compartidas;
    private final ControladorNotificacion controlador = new ControladorNotificacion(new FormateadorNotificacion());
    private final ConfiguracionPomodoro config = new ConfiguracionPomodoro(
            Duration.ofSeconds(10), Duration.ofSeconds(2), Duration.ofSeconds(5), 2);
    private final Instant inicio = Instant.parse("2026-10-25T15:00:00Z");

    @BeforeEach void abrir() {
        factory = Persistence.createEntityManagerFactory("tarea3", Map.of(
                "jakarta.persistence.jdbc.url", "jdbc:h2:mem:notices" + UUID.randomUUID(),
                "hibernate.hbm2ddl.auto", "create-drop", "hibernate.show_sql", "false"));
        individuales = new RepositorioSesionesJpa(factory);
        compartidas = new RepositorioSesionesCompartidasJpa(factory);
    }
    @AfterEach void cerrar() { if (factory != null) factory.close(); }

    private NotificacionFlotante comprobar(SesionEstudio sesion, UUID estudiante, String mensaje) {
        var aviso = controlador.crearNotificacion(sesion, estudiante).orElseThrow();
        assertEquals(mensaje, aviso.mensaje());
        assertEquals(estudiante, aviso.estudianteId());
        assertEquals("CENTRO_APLICACION", aviso.posicion());
        var actual = sesion.getBloques().get(sesion.getBloques().size() - 1);
        assertEquals(actual.getTipo(), aviso.tipoBloque());
        assertEquals(actual.getDuracionObjetivo().toMillis(), aviso.duracionBloqueMillis());
        assertEquals(10500, aviso.duracionMillis());
        return aviso;
    }

    @Test void notificaConcentracionDescansoCortoYLargoEnIndividual() {
        var servicio = new ServicioPomodoro(individuales);
        UUID estudiante = UUID.randomUUID();
        var sesion = servicio.iniciarSesion(estudiante, config, inicio);
        var primero = comprobar(sesion, estudiante, "Es hora de concentrarse. ¡Mucho éxito!");
        servicio.avanzarSiCorresponde(sesion.getId(), 1, inicio.plusSeconds(10));
        sesion = individuales.buscarPorId(sesion.getId()).orElseThrow();
        var descanso = comprobar(sesion, estudiante, "¡El descanso ha comenzado! Disfruta tu tiempo");
        assertNotEquals(primero.id(), descanso.id());
        assertEquals(descanso.id(), controlador.crearNotificacion(sesion, estudiante).orElseThrow().id());
        servicio.avanzarSiCorresponde(sesion.getId(), 2, inicio.plusSeconds(12));
        sesion = individuales.buscarPorId(sesion.getId()).orElseThrow();
        comprobar(sesion, estudiante, "Es hora de concentrarse. ¡Mucho éxito!");
        servicio.avanzarSiCorresponde(sesion.getId(), 3, inicio.plusSeconds(22));
        sesion = individuales.buscarPorId(sesion.getId()).orElseThrow();
        assertEquals(TipoBloque.DESCANSO_LARGO, sesion.getBloques().get(3).getTipo());
        comprobar(sesion, estudiante, "¡El descanso ha comenzado! Disfruta tu tiempo");
    }

    @Test void notificaCambiosRealesParaAmbosParticipantesDeCompartida() {
        var servicio = new ServicioEstudioCompartido(new EmparejadorEnMemoria(), individuales, compartidas, config);
        UUID a = UUID.randomUUID(), b = UUID.randomUUID();
        servicio.solicitarEmparejamiento(a, inicio);
        var sesion = servicio.solicitarEmparejamiento(b, inicio).orElseThrow();
        for (int segundos : new int[]{10,12,22,27}) {
            servicio.actualizarSesion(sesion.getId(), inicio.plusSeconds(segundos));
            var cargada = compartidas.buscarPorId(sesion.getId()).orElseThrow();
            var avisos = new HashSet<String>();
            for (var individual : cargada.getSesiones()) {
                UUID receptor = individual.getEstudiante().getId();
                String mensaje = segundos == 10 || segundos == 22
                        ? "¡El descanso ha comenzado! Disfruta tu tiempo" : "Es hora de concentrarse. ¡Mucho éxito!";
                avisos.add(comprobar(individual, receptor, mensaje).id());
                UUID ajeno = receptor.equals(a) ? b : a;
                assertTrue(controlador.crearNotificacion(individual, ajeno).isEmpty());
            }
            assertEquals(2, avisos.size());
        }
    }

    @Test void noGeneraAvisosSinSesionActivaBloqueEnEjecucionOReceptorCorrecto() {
        UUID estudiante = UUID.randomUUID();
        var servicio = new ServicioPomodoro(individuales);
        assertTrue(controlador.crearNotificacion(null, estudiante).isEmpty());
        var vacia = new SesionEstudio(new Estudiante(estudiante, "Prueba"), config, inicio);
        assertTrue(controlador.crearNotificacion(vacia, estudiante).isEmpty());
        var sesion = servicio.iniciarSesion(estudiante, config, inicio);
        assertTrue(controlador.crearNotificacion(sesion, UUID.randomUUID()).isEmpty());
        servicio.pausarTemporizador(sesion.getId(), inicio.plusSeconds(1));
        assertTrue(controlador.crearNotificacion(individuales.buscarPorId(sesion.getId()).orElseThrow(), estudiante).isEmpty());
        servicio.finalizarSesion(sesion.getId(), inicio.plusSeconds(2));
        assertTrue(controlador.crearNotificacion(individuales.buscarPorId(sesion.getId()).orElseThrow(), estudiante).isEmpty());
    }
}
