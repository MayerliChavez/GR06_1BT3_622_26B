package epn.edu.ec.tdd.chat;

import java.util.UUID;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class ChatTest {

    private ControladorMensajes controlador;
    private ValidadorMensajes validador;

    @Before
    public void setUp() {
        controlador = new ControladorMensajes();
        validador = new ValidadorMensajes();
    }

    @Test
    public void given_valid_message_when_created_then_preserve_text_author_and_session() {
        UUID autorId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID sesionId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        String texto = "Tengo una duda del ejercicio";

        Mensaje mensaje = controlador.crearMensaje(texto, autorId, sesionId);

        assertNotNull("Debe crearse el mensaje con su autor y sesión", mensaje);
        assertEquals(texto, mensaje.texto());
        assertEquals(autorId, mensaje.autorId());
        assertEquals(sesionId, mensaje.sesionId());
    }

    @Test
    public void given_empty_or_spaces_message_when_validated_then_rejected() {
        String mensajeVacio = "";
        String mensajeConEspacios = "   ";

        assertThrows(IllegalArgumentException.class, () -> validador.validar(mensajeVacio));
        assertThrows(IllegalArgumentException.class, () -> validador.validar(mensajeConEspacios));
    }

    @After
    public void tearDown() {
        controlador = null;
        validador = null;
    }
}
