package epn.edu.ec.salas;

import epn.edu.ec.model.TipoBloque;
import epn.edu.ec.service.ServicioCicloSalaPrivada.EstadoCiclo;
import epn.edu.ec.web.SerializadorCicloSala;
import java.time.Instant;
import java.util.UUID;
import org.junit.Test;
import org.junit.Before;
import org.junit.After;

import static org.junit.Assert.*;

public class SerializadorCicloSalaTest extends PruebaSalaPrivada {
    private UUID salaId;

    private Instant ahora;

    @Before
    public void setUp() {
        salaId = UUID.randomUUID();
        ahora = Instant.parse("2026-11-08T15:00:00Z");
    }

    @Test
    public void given_sala_sin_iniciar_when_serializar_then_fechas_nulas_y_tiempo_cero() {
        var estado = new EstadoCiclo(
                salaId,
                false,
                null,
                null,
                null,
                0,
                0,
                ahora
        );

        String json = new SerializadorCicloSala()
                .serializar(estado);

        assertTrue(json.contains("\"tipo\":\"estadoCiclo\""));
        assertTrue(json.contains("\"iniciado\":false"));
        assertTrue(json.contains("\"bloque\":null"));
        assertTrue(json.contains("\"inicioBloque\":null"));
        assertTrue(json.contains("\"finBloque\":null"));
        assertTrue(json.contains("\"restanteMillis\":0"));
    }

    @Test
    public void given_ciclo_iniciado_when_serializar_then_comunica_vencimiento_en_milisegundos() {
        Instant inicio = ahora.minusSeconds(60);
        Instant fin = ahora.plusSeconds(1440);

        var estado = new EstadoCiclo(
                salaId,
                true,
                TipoBloque.CONCENTRACION,
                inicio,
                fin,
                1440000,
                1,
                ahora
        );

        String json = new SerializadorCicloSala()
                .serializar(estado);

        assertTrue(json.contains(
                "\"salaId\":\"" + salaId + "\""
        ));
        assertTrue(json.contains("\"iniciado\":true"));
        assertTrue(json.contains(
                "\"bloque\":\"CONCENTRACION\""
        ));
        assertTrue(json.contains(
                "\"finBloque\":" + fin.toEpochMilli()
        ));
        assertTrue(json.contains(
                "\"horaServidor\":" + ahora.toEpochMilli()
        ));
        assertTrue(json.contains("\"restanteMillis\":1440000"));
        assertTrue(json.contains("\"orden\":1"));
    }

    @After
    public void tearDown() {
        ahora = null;
        salaId = null;
    }
}
