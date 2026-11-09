package epn.edu.ec.salas;

import epn.edu.ec.model.*;
import java.time.Instant;
import java.util.UUID;
import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import static org.junit.Assert.*;

public class DecisionSolicitudSalaTest extends PruebaSalaPrivada {
    private UUID ana;
    private UUID luis;
    private SalaPrivada sala;
    private SolicitudIngresoSala solicitud;

    @Before
    public void setUp() {
        ana = UUID.randomUUID();
        luis = UUID.randomUUID();
        sala = new SalaPrivada(ana, "K7M2QX");
        solicitud = new SolicitudIngresoSala(luis, sala, Instant.parse("2026-11-08T15:00:00Z"));
    }

    @Test
    public void given_solicitud_pendiente_when_responsable_acepta_then_solicitud_aceptada() {
        solicitud.aceptar(ana);
        assertEquals(SolicitudIngresoSala.Estado.ACEPTADA, solicitud.getEstado());
        assertEquals(SalaPrivada.Estado.ABIERTA, sala.getEstado());
    }

    @Test
    public void given_solicitud_pendiente_when_invitado_acepta_then_rechaza_sin_cambiar_estado() {
        assertThrows(SecurityException.class, () -> solicitud.aceptar(luis));
        assertEquals(SolicitudIngresoSala.Estado.PENDIENTE, solicitud.getEstado());
    }

    @Test
    public void given_solicitud_aceptada_when_decidir_de_nuevo_then_rechaza_y_conserva_decision() {
        solicitud.aceptar(ana);
        assertThrows(IllegalStateException.class, () -> solicitud.aceptar(ana));
        assertThrows(IllegalStateException.class, () -> solicitud.rechazar(ana));
        assertEquals(SolicitudIngresoSala.Estado.ACEPTADA, solicitud.getEstado());
    }

    @Test
    public void given_solicitud_rechazada_when_aceptar_then_rechaza_y_conserva_estado() {
        solicitud.rechazar(ana);
        assertThrows(IllegalStateException.class, () -> solicitud.aceptar(ana));
        assertEquals(SolicitudIngresoSala.Estado.RECHAZADA, solicitud.getEstado());
    }

    @After
    public void tearDown() {
        solicitud = null;
        sala = null;
        luis = null;
        ana = null;
    }
}
