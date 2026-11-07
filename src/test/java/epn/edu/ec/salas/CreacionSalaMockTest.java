package epn.edu.ec.salas;

import epn.edu.ec.model.SalaPrivada;
import epn.edu.ec.repository.*;
import epn.edu.ec.service.*;
import java.util.*;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class CreacionSalaMockTest {
    @Test public void unaColisionReintentaYGuardaSoloElCodigoLibre() {
        RepositorioSalasPrivadas salas = mock(RepositorioSalasPrivadas.class);
        RepositorioSesiones sesiones = mock(RepositorioSesiones.class);
        RepositorioSesionesCompartidas compartidas = mock(RepositorioSesionesCompartidas.class);
        GeneradorCodigoSala codigos = mock(GeneradorCodigoSala.class);
        UUID ana = UUID.randomUUID();
        SalaPrivada ocupada = new SalaPrivada(UUID.randomUUID(), "K7M2QX");
        when(salas.buscarAbiertaPorEstudiante(ana)).thenReturn(Optional.empty());
        when(sesiones.buscarActivaPorEstudiante(ana)).thenReturn(Optional.empty());
        when(compartidas.buscarAbiertaPorEstudiante(ana)).thenReturn(Optional.empty());
        when(codigos.generar()).thenReturn("K7M2QX", "P4R8TW");
        when(salas.buscarAbiertaPorCodigo("K7M2QX")).thenReturn(Optional.of(ocupada));
        when(salas.buscarAbiertaPorCodigo("P4R8TW")).thenReturn(Optional.empty());

        SalaPrivada creada = new ServicioSalasPrivadas(salas, sesiones, compartidas, codigos).crear(ana);

        ArgumentCaptor<SalaPrivada> guardada = ArgumentCaptor.forClass(SalaPrivada.class);
        verify(salas).guardarNueva(guardada.capture());
        verify(codigos, times(2)).generar();
        assertSame(creada, guardada.getValue());
        assertEquals("P4R8TW", creada.getCodigo());
        assertEquals("K7M2QX", ocupada.getCodigo());
    }
}
