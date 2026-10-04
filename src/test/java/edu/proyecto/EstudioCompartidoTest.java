package edu.proyecto;
import edu.proyecto.model.*;
import edu.proyecto.repository.*;
import edu.proyecto.service.*;
import jakarta.persistence.*;
import org.junit.jupiter.api.*;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class EstudioCompartidoTest {
    private EntityManagerFactory factory;
    private RepositorioSesionesJpa individuales;
    private RepositorioSesionesCompartidasJpa compartidas;
    private ServicioEstudioCompartido servicio;
    private final Instant inicio = Instant.parse("2026-10-04T00:00:00Z");
    private final ConfiguracionPomodoro config = new ConfiguracionPomodoro(Duration.ofSeconds(10), Duration.ofSeconds(2), Duration.ofSeconds(5), 4);
    @BeforeEach void abrir() {
        factory = Persistence.createEntityManagerFactory("tarea3", Map.of("jakarta.persistence.jdbc.url", "jdbc:h2:mem:shared" + UUID.randomUUID(),
                "hibernate.hbm2ddl.auto", "create-drop", "hibernate.show_sql", "false"));
        individuales = new RepositorioSesionesJpa(factory);
        compartidas = new RepositorioSesionesCompartidasJpa(factory);
        servicio = new ServicioEstudioCompartido(new EmparejadorEnMemoria(), individuales, compartidas, config);
    }
    @AfterEach void cerrar() { if (factory != null) factory.close(); }

    @Test void venceSoloLaParticipacionSinSenalesYConservaAlCompanero() {
        UUID a = UUID.randomUUID(), b = UUID.randomUUID();
        servicio.solicitarEmparejamiento(a, inicio);
        var s = servicio.solicitarEmparejamiento(b, inicio).orElseThrow();
        var presencia = new edu.proyecto.web.PresenciaCompartida(servicio, Duration.ofSeconds(90));
        presencia.registrar(a, s.getId(), inicio);
        presencia.registrar(b, s.getId(), inicio.plusSeconds(60));
        presencia.revisar(inicio.plusSeconds(89));
        assertTrue(compartidas.buscarAbiertaPorEstudiante(a).isPresent());
        presencia.revisar(inicio.plusSeconds(90));
        assertTrue(compartidas.buscarAbiertaPorEstudiante(a).isEmpty());
        assertTrue(compartidas.buscarAbiertaPorEstudiante(b).isPresent());
        var salida = compartidas.buscarPorId(s.getId()).orElseThrow().getParticipaciones().stream()
                .filter(p -> p.getEstudiante().getId().equals(a)).findFirst().orElseThrow();
        assertEquals(MotivoSalida.DESCONEXION, salida.getMotivoSalida());
        presencia.revisar(inicio.plusSeconds(150));
        assertEquals(EstadoSesionCompartida.ABANDONADA, compartidas.buscarPorId(s.getId()).orElseThrow().getEstado());
    }

    @Test void emparejaSinDuplicadosYSinEmparejarConsigoMismo() {
        var cola = new EmparejadorEnMemoria();
        UUID a = UUID.randomUUID(), b = UUID.randomUUID();
        assertTrue(cola.buscarOEsperar(a).isEmpty());
        assertTrue(cola.buscarOEsperar(a).isEmpty());
        assertEquals(a, cola.buscarOEsperar(b).orElseThrow());
        assertTrue(cola.buscarOEsperar(a).isEmpty());
        cola.cancelarEspera(a);
        assertTrue(cola.buscarOEsperar(b).isEmpty());
    }
    @Test void persisteDosSesionesConUnMismoInicioYBloquesSincronizados() {
        UUID a = UUID.randomUUID(), b = UUID.randomUUID();
        assertTrue(servicio.solicitarEmparejamiento(a, inicio).isEmpty());
        var s = servicio.solicitarEmparejamiento(b, inicio.plusSeconds(1)).orElseThrow();
        assertEquals(2, s.getSesiones().size());
        assertEquals(s.getId(), compartidas.buscarAbiertaPorEstudiante(a).orElseThrow().getId());
        assertEquals(s.getId(), servicio.solicitarEmparejamiento(a, inicio.plusSeconds(2)).orElseThrow().getId());
        assertEquals(1, individuales.buscarPorEstudiante(a).size());
        assertEquals(1, individuales.buscarPorEstudiante(b).size());
        servicio.actualizarSesion(s.getId(), inicio.plusSeconds(12));
        var guardada = compartidas.buscarPorId(s.getId()).orElseThrow();
        for (var individual : guardada.getSesiones()) {
            assertEquals(2, individual.getBloques().size());
            assertEquals(TipoBloque.DESCANSO_CORTO, individual.getBloques().get(1).getTipo());
            assertEquals(inicio.plusSeconds(11), individual.getBloques().get(1).getFechaInicio());
            assertEquals(Duration.ofSeconds(10), individual.tiempoEstudiado(inicio.plusSeconds(12)));
        }
        assertEquals(Duration.ofSeconds(1), servicio.consultarTiempoRestante(s.getId(), inicio.plusSeconds(12)).orElseThrow());
        servicio.actualizarSesion(s.getId(), inicio.plusSeconds(13));
        assertEquals(TipoBloque.CONCENTRACION, compartidas.buscarPorId(s.getId()).orElseThrow().getSesiones().get(0).getBloques().get(2).getTipo());
    }
    @Test void salidaYDesconexionConservanHistorialYPermitenContinuarAlOtro() {
        UUID a = UUID.randomUUID(), b = UUID.randomUUID();
        servicio.solicitarEmparejamiento(a, inicio);
        var s = servicio.solicitarEmparejamiento(b, inicio).orElseThrow();
        servicio.salir(s.getId(), a, inicio.plusSeconds(3));
        assertTrue(compartidas.buscarAbiertaPorEstudiante(a).isEmpty());
        assertTrue(compartidas.buscarAbiertaPorEstudiante(b).isPresent());
        assertEquals(EstadoSesion.FINALIZADA, individuales.buscarPorEstudiante(a).get(0).getEstado());
        assertEquals(Duration.ofSeconds(3), individuales.buscarPorEstudiante(a).get(0).tiempoEstudiado(inicio.plusSeconds(20)));
        servicio.registrarDesconexion(s.getId(), b, inicio.plusSeconds(4));
        var cerrada = compartidas.buscarPorId(s.getId()).orElseThrow();
        assertEquals(EstadoSesionCompartida.CERRADA, cerrada.getEstado());
        assertEquals(EstadoSesion.ABANDONADA, individuales.buscarPorEstudiante(b).get(0).getEstado());
        assertTrue(servicio.consultarTiempoRestante(s.getId(), inicio.plusSeconds(10)).isEmpty());
    }
    @Test void ambasDesconexionesAbandonanLaCompartida() {
        UUID a = UUID.randomUUID(), b = UUID.randomUUID();
        servicio.solicitarEmparejamiento(a, inicio);
        var s = servicio.solicitarEmparejamiento(b, inicio).orElseThrow();
        servicio.registrarDesconexion(s.getId(), a, inicio.plusSeconds(2));
        servicio.registrarDesconexion(s.getId(), b, inicio.plusSeconds(3));
        assertEquals(EstadoSesionCompartida.ABANDONADA, compartidas.buscarPorId(s.getId()).orElseThrow().getEstado());
    }
    @Test void rechazaSesionesIndividualesActivasYParticipantesAjenos() {
        UUID a = UUID.randomUUID(), b = UUID.randomUUID();
        new ServicioPomodoro(individuales).iniciarSesion(a, config, inicio);
        assertThrows(IllegalStateException.class, () -> servicio.solicitarEmparejamiento(a, inicio.plusSeconds(1)));
        servicio.solicitarEmparejamiento(b, inicio);
        var s = servicio.solicitarEmparejamiento(UUID.randomUUID(), inicio).orElseThrow();
        assertThrows(IllegalArgumentException.class, () -> servicio.salir(s.getId(), a, inicio.plusSeconds(1)));
        assertEquals(EstadoSesionCompartida.ACTIVA, compartidas.buscarPorId(s.getId()).orElseThrow().getEstado());
    }
}
