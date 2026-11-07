package epn.edu.ec.salas;

import epn.edu.ec.model.*;
import epn.edu.ec.repository.*;
import epn.edu.ec.service.*;
import java.time.*;
import java.util.*;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class ServicioSalasPrivadasTest {
    private final UUID ana = UUID.randomUUID();
    private final Map<UUID, SalaPrivada> almacen = new HashMap<>();
    private SesionEstudio individual;
    private SesionCompartida emparejada;
    private ServicioSalasPrivadas servicio;

    @Before public void preparar() {
        RepositorioSalasPrivadas salas = new RepositorioSalasPrivadas() {
            public Optional<SalaPrivada> buscarAbiertaPorEstudiante(UUID id) {
                return almacen.values().stream().filter(s -> s.getResponsableId().equals(id)).findFirst();
            }
            public Optional<SalaPrivada> buscarAbiertaPorCodigo(String codigo) {
                return almacen.values().stream().filter(s -> s.getCodigo().equals(codigo)).findFirst();
            }
            public void guardarNueva(SalaPrivada sala) { almacen.put(sala.getId(), sala); }
        };
        RepositorioSesiones sesiones = new RepositorioSesiones() {
            public void guardar(SesionEstudio s) { individual = s; }
            public Optional<SesionEstudio> buscarPorId(UUID id) { return Optional.empty(); }
            public List<SesionEstudio> buscarPorEstudiante(UUID id) { return List.of(); }
            public Optional<SesionEstudio> buscarActivaPorEstudiante(UUID id) { return Optional.ofNullable(individual); }
        };
        RepositorioSesionesCompartidas compartidas = new RepositorioSesionesCompartidas() {
            public void guardar(SesionCompartida s) { emparejada = s; }
            public Optional<SesionCompartida> buscarPorId(UUID id) { return Optional.empty(); }
            public Optional<SesionCompartida> buscarAbiertaPorEstudiante(UUID id) { return Optional.ofNullable(emparejada); }
        };
        servicio = new ServicioSalasPrivadas(salas, sesiones, compartidas, () -> "K7M2QX");
    }

    @Test public void crearYConsultarConservaLaMismaSalaYCodigo() {
        SalaPrivada creada = servicio.crear(ana);
        SalaPrivada recuperada = servicio.consultarActual(ana).orElseThrow();
        assertEquals(creada.getId(), recuperada.getId());
        assertEquals("K7M2QX", recuperada.getCodigo());
        assertEquals(ana, recuperada.getResponsableId());
        assertEquals(SalaPrivada.Estado.ABIERTA, recuperada.getEstado());
        assertEquals(1, almacen.size());
    }

    @Test public void unaSegundaCreacionNoDuplicaLaSalaExistente() {
        SalaPrivada primera = servicio.crear(ana);
        assertThrows(IllegalStateException.class, () -> servicio.crear(ana));
        assertEquals(primera.getId(), servicio.consultarActual(ana).orElseThrow().getId());
        assertEquals(1, almacen.size());
    }

    @Test public void sesionIndividualImpideCrearYSigueSinCambios() {
        Instant inicio = Instant.parse("2026-11-07T15:00:00Z");
        individual = sesion(ana, inicio);
        individual.iniciarSiguienteBloque(inicio);
        IllegalStateException error = assertThrows(IllegalStateException.class, () -> servicio.crear(ana));
        assertEquals("Ya tienes una sesión de estudio en curso. Finalízala antes de crear una sala.", error.getMessage());
        assertEquals(Duration.ofMinutes(10), individual.tiempoRestanteBloqueActual(inicio.plusSeconds(900)).orElseThrow());
        assertTrue(almacen.isEmpty());
    }

    @Test public void sesionEmparejadaImpideCrearOtraSala() {
        Instant inicio = Instant.parse("2026-11-07T15:00:00Z");
        emparejada = new SesionCompartida(sesion(ana, inicio), sesion(UUID.randomUUID(), inicio));
        emparejada.iniciar(inicio);
        assertThrows(IllegalStateException.class, () -> servicio.crear(ana));
        assertTrue(almacen.isEmpty());
        assertEquals(EstadoSesionCompartida.ACTIVA, emparejada.getEstado());
    }

    private SesionEstudio sesion(UUID id, Instant inicio) {
        return new SesionEstudio(new Estudiante(id, "Estudiante"),
                new ConfiguracionPomodoro(Duration.ofMinutes(25), Duration.ofMinutes(5), Duration.ofMinutes(15), 4), inicio);
    }
}
