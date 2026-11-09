package epn.edu.ec.salas;

import epn.edu.ec.model.*;
import java.time.Instant;
import java.util.UUID;
import org.junit.Test;
import org.junit.Before;
import org.junit.After;

import static org.junit.Assert.*;

public class CierreSalaPrivadaTest extends PruebaSalaPrivada {
    private SalaPrivada sala;

    private UUID responsable;

    private Instant ahora;

    @Before
    public void setUp() {
        responsable = UUID.randomUUID();
        ahora = Instant.parse("2026-11-08T15:18:00Z");
        sala = new SalaPrivada(responsable, "K7M2QX");
    }

    @Test
    public void given_sala_abierta_when_responsable_cierra_then_conserva_fecha_y_bloquea_cambios() {

        sala.cerrar(responsable, ahora);

        assertEquals(SalaPrivada.Estado.CERRADA, sala.getEstado());
        assertEquals(ahora, sala.getFechaCierre());

        assertThrows(
                IllegalStateException.class,
                () -> sala.actualizarBloqueActual(
                        TipoBloque.CONCENTRACION
                )
        );
    }

    @Test
    public void given_estudiante_ajeno_when_cerrar_sala_then_rechaza_y_conserva_sala_abierta() {

        assertThrows(
                SecurityException.class,
                () -> sala.cerrar(UUID.randomUUID(), ahora)
        );

        assertEquals(SalaPrivada.Estado.ABIERTA, sala.getEstado());
        assertNull(sala.getFechaCierre());
    }

    @After
    public void tearDown() {
        sala = null;
        ahora = null;
        responsable = null;
    }
}
