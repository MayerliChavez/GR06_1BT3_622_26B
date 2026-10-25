package epn.edu.ec;

import epn.edu.ec.model.*;
import epn.edu.ec.repository.RepositorioSesiones;
import epn.edu.ec.service.ServicioPomodoro;
import org.junit.Before;
import org.junit.Test;
import java.time.*;
import java.util.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class TransicionAutomaticaTest {

    private final Instant inicio = Instant.parse("2026-10-18T15:00:00Z");
    private SesionEstudio sesion;
    private ServicioPomodoro servicio;
    private RepositorioSesiones repositorio;

    @Before public void setUp() {
        sesion = new SesionEstudio(new Estudiante(UUID.randomUUID(), "Estudiante"),
                new ConfiguracionPomodoro(Duration.ofSeconds(10), Duration.ofSeconds(2), Duration.ofSeconds(5), 2), inicio);
        sesion.iniciarSiguienteBloque(inicio);
        repositorio = mock(RepositorioSesiones.class);
        when(repositorio.buscarPorId(sesion.getId())).thenReturn(Optional.of(sesion));
        servicio = new ServicioPomodoro(repositorio);
    }

    @Test public void alternaConcentracionDescansoCortoYDescansoLargoSinClic() {
        servicio.avanzarSiCorresponde(sesion.getId(), 1, inicio.plusSeconds(10));
        assertEquals(TipoBloque.DESCANSO_CORTO, sesion.getBloques().get(1).getTipo());
        servicio.avanzarSiCorresponde(sesion.getId(), 2, inicio.plusSeconds(12));
        assertEquals(TipoBloque.CONCENTRACION, sesion.getBloques().get(2).getTipo());
        servicio.avanzarSiCorresponde(sesion.getId(), 3, inicio.plusSeconds(22));
        assertEquals(TipoBloque.DESCANSO_LARGO, sesion.getBloques().get(3).getTipo());
        servicio.avanzarSiCorresponde(sesion.getId(), 4, inicio.plusSeconds(27));
        assertEquals(TipoBloque.CONCENTRACION, sesion.getBloques().get(4).getTipo());
        assertEquals(EstadoBloque.EN_EJECUCION, sesion.getBloques().get(4).getEstado());
    }

    @Test public void dosPestanasNoDuplicanLaTransicion() {
        servicio.avanzarSiCorresponde(sesion.getId(), 1, inicio.plusSeconds(10));
        servicio.avanzarSiCorresponde(sesion.getId(), 1, inicio.plusSeconds(100));
        assertEquals(2, sesion.getBloques().size());
        verify(repositorio, times(1)).guardar(sesion);
    }

    @Test public void noAvanzaAntesDeTiempoNiDurantePausa() {
        servicio.avanzarSiCorresponde(sesion.getId(), 1, inicio.plusSeconds(9));
        sesion.pausar(inicio.plusSeconds(9));
        servicio.avanzarSiCorresponde(sesion.getId(), 1, inicio.plusSeconds(100));
        assertEquals(1, sesion.getBloques().size());
        assertEquals(EstadoBloque.PAUSADO, sesion.getBloques().get(0).getEstado());
        verify(repositorio, never()).guardar(any());
    }
}
