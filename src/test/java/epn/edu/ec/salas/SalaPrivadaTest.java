package epn.edu.ec.salas;

import epn.edu.ec.model.SalaPrivada;
import java.util.UUID;
import org.junit.Test;
import org.junit.Before;
import org.junit.After;

import static org.junit.Assert.*;

public class SalaPrivadaTest extends PruebaSalaPrivada {

    private UUID responsableId;

    @Before
    public void setUp() {
        responsableId = UUID.randomUUID();
    }

    @Test
    public void given_responsable_y_codigo_when_crear_sala_then_sala_abierta_con_datos_asignados() {

        SalaPrivada sala =
                new SalaPrivada(responsableId, "K7M2QX");

        assertNotNull(sala.getId());
        assertEquals(responsableId, sala.getResponsableId());
        assertEquals("K7M2QX", sala.getCodigo());
        assertEquals(SalaPrivada.Estado.ABIERTA, sala.getEstado());
    }

    @After
    public void tearDown() {
        responsableId = null;
    }
}
