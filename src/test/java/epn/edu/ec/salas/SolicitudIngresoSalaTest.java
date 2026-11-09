package epn.edu.ec.salas;

import epn.edu.ec.model.SalaPrivada;
import epn.edu.ec.model.SolicitudIngresoSala;
import java.time.Instant;
import java.util.UUID;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class SolicitudIngresoSalaTest extends PruebaSalaPrivada {

    private UUID ana;
    private UUID luis;
    private SalaPrivada sala;
    private Instant fecha;
    private SolicitudIngresoSala solicitud;

    @Before
    public void setUp() {
        ana = UUID.randomUUID();
        luis = UUID.randomUUID();
        sala = new SalaPrivada(ana, "K7M2QX");
        fecha = Instant.parse("2026-11-07T15:00:00Z");
        solicitud = new SolicitudIngresoSala(luis, sala, fecha);
    }

    @Test
    public void given_estudiante_y_sala_when_crear_solicitud_then_pendiente_sin_modificar_sala() {
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
    public void given_solicitud_pendiente_when_estudiante_cancela_then_cancelada_y_no_repetible() {
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
    public void given_solicitud_pendiente_when_responsable_rechaza_then_rechazada_y_sala_abierta() {
        solicitud.rechazar(ana);

        assertEquals(
                SolicitudIngresoSala.Estado.RECHAZADA,
                solicitud.getEstado()
        );

        assertEquals(SalaPrivada.Estado.ABIERTA, sala.getEstado());
    }


    @After
    public void tearDown() {
        solicitud = null;
        sala = null;
        ana = null;
        luis = null;
        fecha = null;
    }
}
