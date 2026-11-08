package epn.edu.ec.salas;

import epn.edu.ec.model.*;
import epn.edu.ec.service.ServicioCicloSalaPrivada.EstadoCiclo;
import epn.edu.ec.web.*;
import java.time.Instant;
import java.util.*;
import org.junit.Test;

import static org.junit.Assert.*;

public class SerializadorChatSalaTest {
    private final Instant ahora =
            Instant.parse("2026-11-08T15:00:00Z");

    private final Estudiante ana =
            new Estudiante(UUID.randomUUID(), "Ana \"A\"");

    private final SalaPrivada sala =
            new SalaPrivada(ana.getId(), "K7M2QX");

    @Test
    public void mensajePrivadoEscapaTextoYConservaAutorYFecha() {
        var mensaje = new MensajeSalaPrivada(
                "Hola \"grupo\"\n¿Listos?",
                ana,
                sala,
                ahora
        );

        String json = new SerializadorChat().mensaje(mensaje);

        assertTrue(json.contains(
                "\"texto\":\"Hola \\\"grupo\\\"\\n¿Listos?\""
        ));
        assertTrue(json.contains(
                "\"autor\":\"Ana \\\"A\\\"\""
        ));
        assertTrue(json.contains(
                "\"autorId\":\"" + ana.getId() + "\""
        ));
        assertTrue(json.contains(
                "\"fecha\":\"" + ahora + "\""
        ));
        assertFalse(json.contains("\n"));
    }

    @Test
    public void estadoSinMensajesMantieneElProtocoloDelReloj() {
        String json = new SerializadorCicloSala().serializar(
                estado(),
                List.of(),
                List.of()
        );

        assertTrue(json.contains("\"tipo\":\"estadoCiclo\""));
        assertTrue(json.contains(
                "\"salaId\":\"" + sala.getId() + "\""
        ));
        assertTrue(json.contains("\"mensajes\":[]"));
        assertTrue(json.contains("\"eventosChat\":[]"));
    }

    @Test
    public void confirmacionYErrorViajanSinRomperElJson() {
        var mensaje =
                new MensajeSalaPrivada("Hola", ana, sala, ahora);

        String json = new SerializadorCicloSala().serializar(
                estado(),
                List.of(mensaje),
                List.of(
                        new SerializadorCicloSala.EventoChat(
                                "guardado", 7L, null
                        ),
                        new SerializadorCicloSala.EventoChat(
                                "error",
                                null,
                                "Escribe \"contenido\"\npor favor"
                        )
                )
        );

        assertTrue(json.contains("\"mensajes\":[{\"id\":"));
        assertTrue(json.contains(
                "\"tipo\":\"guardado\",\"mensajeId\":7"
        ));
        assertTrue(json.contains("\"tipo\":\"error\""));
        assertTrue(json.contains(
                "Escribe \\\"contenido\\\"\\npor favor"
        ));
        assertFalse(json.contains("\n"));
    }

    private EstadoCiclo estado() {
        return new EstadoCiclo(
                sala.getId(),
                false,
                null,
                null,
                null,
                0,
                0,
                ahora
        );
    }
}