package epn.edu.ec.salas;

import epn.edu.ec.model.SalaPrivada;
import epn.edu.ec.repository.*;
import epn.edu.ec.service.*;
import java.util.*;
import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import org.mockito.ArgumentCaptor;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class CreacionSalaMockTest extends PruebaSalaPrivada {
    private RepositorioSalasPrivadas salas;
    private RepositorioSesiones sesiones;
    private RepositorioSesionesCompartidas compartidas;
    private GeneradorCodigoSala codigos;
    private UUID ana;
    private ServicioSalasPrivadas servicio;

    @Before
    public void setUp() {
        salas = mock(RepositorioSalasPrivadas.class);
        sesiones = mock(RepositorioSesiones.class);
        compartidas = mock(RepositorioSesionesCompartidas.class);
        codigos = mock(GeneradorCodigoSala.class);
        ana = UUID.randomUUID();

        servicio = new ServicioSalasPrivadas(salas, sesiones, compartidas, codigos);
    }

    @Test
    public void given_codigo_ocupado_when_crear_sala_then_reintenta_y_guarda_solo_codigo_libre() {
        SalaPrivada ocupada = new SalaPrivada(UUID.randomUUID(), "K7M2QX");
        when(salas.buscarAbiertaPorEstudiante(ana)).thenReturn(Optional.empty());
        when(sesiones.buscarActivaPorEstudiante(ana)).thenReturn(Optional.empty());
        when(compartidas.buscarAbiertaPorEstudiante(ana)).thenReturn(Optional.empty());

        when(codigos.generar()).thenReturn("K7M2QX", "P4R8TW");
        when(salas.buscarAbiertaPorCodigo("K7M2QX")).thenReturn(Optional.of(ocupada));
        when(salas.buscarAbiertaPorCodigo("P4R8TW")).thenReturn(Optional.empty());

        SalaPrivada creada = servicio.crear(ana);

        ArgumentCaptor<SalaPrivada> guardada = ArgumentCaptor.forClass(SalaPrivada.class);
        verify(salas).guardarNueva(guardada.capture());
        verify(codigos, times(2)).generar();
        assertSame(creada, guardada.getValue());
        assertEquals("P4R8TW", creada.getCodigo());
        assertEquals("K7M2QX", ocupada.getCodigo());
    }

    @After
    public void tearDown() {
        validateMockitoUsage();
        servicio = null;
        salas = null;
        sesiones = null;
        compartidas = null;
        codigos = null;
        ana = null;
    }
}
