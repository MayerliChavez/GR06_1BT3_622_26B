package epn.edu.ec.salas;

import epn.edu.ec.model.TipoBloque;
import epn.edu.ec.service.ServicioCicloSalaPrivada.EstadoCiclo;
import epn.edu.ec.web.SerializadorCicloSala;
import java.time.Instant;
import java.util.UUID;
import org.junit.Test;

import static org.junit.Assert.*;

public class SerializadorCicloSalaTest {
    private final UUID salaId = UUID.randomUUID();

    private final Instant ahora =
            Instant.parse("2026-11-08T15:00:00Z");

    @Test
    public void salaSinIniciarSeComunicaSinFechasInventadas() {
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
    public void cicloIniciadoComunicaVencimientoEnMilisegundos() {
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
}