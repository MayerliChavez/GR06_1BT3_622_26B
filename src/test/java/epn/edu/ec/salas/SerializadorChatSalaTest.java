package epn.edu.ec.salas;

import epn.edu.ec.model.*;
import epn.edu.ec.service.ServicioCicloSalaPrivada.EstadoCiclo;
import epn.edu.ec.web.*;
import java.time.Instant;
import java.util.*;
import org.junit.Test;
import org.junit.Before;
import org.junit.After;

import static org.junit.Assert.*;

public class SerializadorChatSalaTest extends PruebaSalaPrivada {
    private Instant ahora;

    private Estudiante ana;

    private SalaPrivada sala;

    @Before
    public void setUp() {
        ahora = Instant.parse("2026-11-08T15:00:00Z");
        ana = new Estudiante(UUID.randomUUID(), "Ana \"A\"");
        sala = new SalaPrivada(ana.getId(), "K7M2QX");
    }

    @Test
    public void given_mensaje_con_caracteres_especiales_when_serializar_then_escapa_y_conserva_autor_y_fecha() {
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
    public void given_ciclo_sin_mensajes_when_serializar_then_conserva_protocolo_del_reloj() {
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
    public void given_confirmacion_y_error_when_serializar_then_eventos_con_json_escapado() {
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

    @After
    public void tearDown() {
        sala = null;
        ana = null;
        ahora = null;
    }
}
