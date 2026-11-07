package epn.edu.ec.tdd.chat;

import java.util.Arrays;
import java.util.Collection;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

/** P02: contenido de mensajes de HU08, según la planificación v3. */
@RunWith(Parameterized.class)
public class ValidadorMensajesParametersTest {
    private final String texto;
    private final String errorEsperado;

    @Parameterized.Parameters(name = "{index}: {0}")
    public static Collection<Object[]> casos() {
        String vacio = "Escribe un mensaje antes de enviarlo.";
        return Arrays.asList(new Object[][] {
            {"campo ausente", null, vacio},
            {"mensaje vacío", "", vacio},
            {"solo espacios", "   ", vacio},
            {"un carácter", "A", null},
            {"1999 caracteres", "A".repeat(1999), null},
            {"2000 caracteres completos", "A".repeat(2000), null},
            {"2001 caracteres", "A".repeat(2001),
                "El mensaje no puede superar los 2000 caracteres."}
        });
    }

    public ValidadorMensajesParametersTest(String descripcion, String texto,
            String errorEsperado) {
        this.texto = texto;
        this.errorEsperado = errorEsperado;
    }

    @Test
    public void validarContenidoSegunLimitesYMensajesDeHu08() {
        ValidadorMensajes validador = new ValidadorMensajes();
        if (errorEsperado == null) {
            validador.validar(texto);
        } else {
            IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                    () -> validador.validar(texto));
            assertEquals(errorEsperado, error.getMessage());
        }
    }
}
