package epn.edu.ec.salas;

import epn.edu.ec.model.TipoBloque;
import epn.edu.ec.service.PoliticaAdmisionSala;
import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import static org.junit.Assert.*;

/** Contrato común de los controles web y de la aceptación transaccional. */
public class PoliticaAdmisionSalaTest extends PruebaSalaPrivada {
    private PoliticaAdmisionSala politica;

    @Before
    public void setUp() {
        politica = new PoliticaAdmisionSala();
    }

    @Test
    public void given_sala_llena_en_concentracion_when_evaluar_admision_then_informa_falta_de_cupo() {
        var decision = politica.evaluar(TipoBloque.CONCENTRACION, 4, true);
        assertFalse(decision.permitida());
        assertEquals("La sala está llena. No es posible aceptar más participantes.", decision.mensaje());
        var error = assertThrows(IllegalStateException.class,
                () -> politica.exigirAceptacion(TipoBloque.CONCENTRACION, 4, true));
        assertEquals(decision.mensaje(), error.getMessage());
    }

    @Test
    public void given_concentracion_con_cupo_when_evaluar_admision_then_indica_siguiente_descanso() {
        var decision = politica.evaluar(TipoBloque.CONCENTRACION, 2, true);
        assertFalse(decision.permitida());
        assertEquals("Podrás aceptar nuevos participantes en el siguiente descanso.", decision.mensaje());
        var error = assertThrows(IllegalStateException.class,
                () -> politica.exigirAceptacion(TipoBloque.CONCENTRACION, 2, true));
        assertEquals(decision.mensaje(), error.getMessage());
    }

    @Test
    public void given_invitado_y_cupo_when_exigir_aceptacion_then_rechaza_por_permisos() {
        var decision = politica.evaluar(TipoBloque.DESCANSO_CORTO, 2, false);
        assertFalse(decision.permitida());
        var error = assertThrows(SecurityException.class,
                () -> politica.exigirAceptacion(TipoBloque.DESCANSO_CORTO, 2, false));
        assertEquals(decision.mensaje(), error.getMessage());
    }

    @Test
    public void given_sala_sin_iniciar_o_en_descanso_when_exigir_aceptacion_then_permite_con_cupo() {
        for (TipoBloque bloque : new TipoBloque[] {
                null, TipoBloque.DESCANSO_CORTO, TipoBloque.DESCANSO_LARGO}) {
            assertTrue(politica.evaluar(bloque, 3, true).permitida());
            assertEquals("", politica.evaluar(bloque, 3, true).mensaje());
            politica.exigirAceptacion(bloque, 3, true);
        }
    }

    @Test
    public void given_ocupacion_invalida_when_evaluar_admision_then_rechaza_ingreso() {
        for (int ocupacion : new int[] {-1, 0, 5}) {
            assertFalse(politica.evaluar(null, ocupacion, true).permitida());
            assertThrows(IllegalStateException.class,
                    () -> politica.exigirAceptacion(null, ocupacion, true));
        }
    }

    @After
    public void tearDown() {
        politica = null;
    }
}
