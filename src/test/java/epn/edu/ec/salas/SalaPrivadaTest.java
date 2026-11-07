package epn.edu.ec.salas;

import epn.edu.ec.model.SalaPrivada;
import java.util.UUID;
import org.junit.Test;

import static org.junit.Assert.*;

public class SalaPrivadaTest {

    @Test
    public void crearSalaAsignaResponsableCodigoYEstadoAbierto() {
        UUID responsableId = UUID.randomUUID();

        SalaPrivada sala =
                new SalaPrivada(responsableId, "K7M2QX");

        assertNotNull(sala.getId());
        assertEquals(responsableId, sala.getResponsableId());
        assertEquals("K7M2QX", sala.getCodigo());
        assertEquals(SalaPrivada.Estado.ABIERTA, sala.getEstado());
    }
}
