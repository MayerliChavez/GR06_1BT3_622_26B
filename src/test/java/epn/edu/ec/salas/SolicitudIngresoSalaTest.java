package epn.edu.ec.salas;

import epn.edu.ec.model.SalaPrivada;
import epn.edu.ec.model.SolicitudIngresoSala;
import java.time.Instant;
import java.util.UUID;
import org.junit.Test;

import static org.junit.Assert.*;

public class SolicitudIngresoSalaTest {

    private final UUID ana = UUID.randomUUID();
    private final UUID luis = UUID.randomUUID();

    private final SalaPrivada sala =
            new SalaPrivada(ana, "K7M2QX");

    private final Instant fecha =
            Instant.parse("2026-11-07T15:00:00Z");

    @Test
    public void crearSolicitudLaDejaPendienteSinModificarLaSala() {
        SolicitudIngresoSala solicitud =
                new SolicitudIngresoSala(luis, sala, fecha);

        assertNotNull(solicitud.getId());
        assertEquals(luis, solicitud.getEstudianteId());
        assertEquals(sala.getId(), solicitud.getSala().getId());
        assertEquals(fecha, solicitud.getFechaSolicitud());
        assertEquals(
                SolicitudIngresoSala.Estado.PENDIENTE,
                solicitud.getEstado()
        );

        assertEquals(SalaPrivada.Estado.ABIERTA, sala.getEstado());
        assertEquals(ana, sala.getResponsableId());
    }

    @Test
    public void estudiantePuedeCancelarSuSolicitudPendiente() {
        SolicitudIngresoSala solicitud =
                new SolicitudIngresoSala(luis, sala, fecha);

        solicitud.cancelar(luis);

        assertEquals(
                SolicitudIngresoSala.Estado.CANCELADA,
                solicitud.getEstado()
        );

        assertThrows(
                IllegalStateException.class,
                () -> solicitud.cancelar(luis)
        );
    }

    @Test
    public void responsablePuedeRechazarLaSolicitudPendiente() {
        SolicitudIngresoSala solicitud =
                new SolicitudIngresoSala(luis, sala, fecha);

        solicitud.rechazar(ana);

        assertEquals(
                SolicitudIngresoSala.Estado.RECHAZADA,
                solicitud.getEstado()
        );

        assertEquals(SalaPrivada.Estado.ABIERTA, sala.getEstado());
    }
}