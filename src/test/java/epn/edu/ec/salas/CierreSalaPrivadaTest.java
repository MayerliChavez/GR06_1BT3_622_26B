package epn.edu.ec.salas;

import epn.edu.ec.model.*;
import java.time.Instant;
import java.util.UUID;
import org.junit.Test;

import static org.junit.Assert.*;

public class CierreSalaPrivadaTest {

    private final UUID responsable = UUID.randomUUID();

    private final Instant ahora =
            Instant.parse("2026-11-08T15:18:00Z");

    @Test
    public void responsableCierraYConservaLaFecha() {
        var sala = new SalaPrivada(responsable, "K7M2QX");

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
    public void otroEstudianteNoPuedeCerrarLaSala() {
        var sala = new SalaPrivada(responsable, "K7M2QX");

        assertThrows(
                SecurityException.class,
                () -> sala.cerrar(UUID.randomUUID(), ahora)
        );

        assertEquals(SalaPrivada.Estado.ABIERTA, sala.getEstado());
        assertNull(sala.getFechaCierre());
    }
}