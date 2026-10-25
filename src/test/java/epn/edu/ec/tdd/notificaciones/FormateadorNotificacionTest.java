package epn.edu.ec.tdd.notificaciones;

import epn.edu.ec.model.TipoBloque;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class FormateadorNotificacionTest {

    private FormateadorNotificacion formateador;

    @Before
    public void setUp() {
        formateador = new FormateadorNotificacion();
    }

    @Test
    public void given_block_types_when_formatting_notification_then_expected_messages() {
        String mensajeConcentracion = "Es hora de concentrarse. ¡Mucho éxito!";
        String mensajeDescanso = "¡El descanso ha comenzado! Disfruta tu tiempo";

        String concentracion = formateador.formatear(TipoBloque.CONCENTRACION);
        String descansoCorto = formateador.formatear(TipoBloque.DESCANSO_CORTO);
        String descansoLargo = formateador.formatear(TipoBloque.DESCANSO_LARGO);

        assertEquals(mensajeConcentracion, concentracion);
        assertEquals(mensajeDescanso, descansoCorto);
        assertEquals(mensajeDescanso, descansoLargo);
    }

    @After
    public void tearDown() {
        formateador = null;
    }
}
