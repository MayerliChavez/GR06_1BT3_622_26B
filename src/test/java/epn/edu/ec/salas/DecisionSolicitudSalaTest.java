package epn.edu.ec.salas;

import epn.edu.ec.model.*;
import java.time.Instant;
import java.util.UUID;
import org.junit.Test;
import static org.junit.Assert.*;

public class DecisionSolicitudSalaTest {
    private final UUID ana = UUID.randomUUID();
    private final UUID luis = UUID.randomUUID();
    private final SalaPrivada sala = new SalaPrivada(ana, "K7M2QX");
    private final SolicitudIngresoSala solicitud = new SolicitudIngresoSala(luis, sala, Instant.parse("2026-11-08T15:00:00Z"));

    @Test public void responsableAceptaSolicitudPendiente() {
        solicitud.aceptar(ana);
        assertEquals(SolicitudIngresoSala.Estado.ACEPTADA, solicitud.getEstado());
        assertEquals(SalaPrivada.Estado.ABIERTA, sala.getEstado());
    }

    @Test public void invitadoNoPuedeAceptarNiCambiarLaSolicitud() {
        assertThrows(SecurityException.class, () -> solicitud.aceptar(luis));
        assertEquals(SolicitudIngresoSala.Estado.PENDIENTE, solicitud.getEstado());
    }

    @Test public void unaAceptadaNoPuedeDecidirseDeNuevo() {
        solicitud.aceptar(ana);
        assertThrows(IllegalStateException.class, () -> solicitud.aceptar(ana));
        assertThrows(IllegalStateException.class, () -> solicitud.rechazar(ana));
        assertEquals(SolicitudIngresoSala.Estado.ACEPTADA, solicitud.getEstado());
    }

    @Test public void unaRechazadaNoPuedeAceptarse() {
        solicitud.rechazar(ana);
        assertThrows(IllegalStateException.class, () -> solicitud.aceptar(ana));
        assertEquals(SolicitudIngresoSala.Estado.RECHAZADA, solicitud.getEstado());
    }
}
