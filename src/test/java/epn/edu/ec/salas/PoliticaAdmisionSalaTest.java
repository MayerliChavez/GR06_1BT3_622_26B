package epn.edu.ec.salas;

import epn.edu.ec.model.TipoBloque;
import epn.edu.ec.service.PoliticaAdmisionSala;
import org.junit.Test;
import static org.junit.Assert.*;

/** Contrato común de los controles web y de la aceptación transaccional. */
public class PoliticaAdmisionSalaTest {
    private final PoliticaAdmisionSala politica = new PoliticaAdmisionSala();

    @Test public void salaLlenaExplicaElCupoAunqueEsteEnConcentracion() {
        var decision = politica.evaluar(TipoBloque.CONCENTRACION, 4, true);
        assertFalse(decision.permitida());
        assertEquals("La sala está llena. No es posible aceptar más participantes.", decision.mensaje());
        var error = assertThrows(IllegalStateException.class,
                () -> politica.exigirAceptacion(TipoBloque.CONCENTRACION, 4, true));
        assertEquals(decision.mensaje(), error.getMessage());
    }

    @Test public void concentracionConCupoIndicaElSiguienteDescanso() {
        var decision = politica.evaluar(TipoBloque.CONCENTRACION, 2, true);
        assertFalse(decision.permitida());
        assertEquals("Podrás aceptar nuevos participantes en el siguiente descanso.", decision.mensaje());
        var error = assertThrows(IllegalStateException.class,
                () -> politica.exigirAceptacion(TipoBloque.CONCENTRACION, 2, true));
        assertEquals(decision.mensaje(), error.getMessage());
    }

    @Test public void unInvitadoNoObtienePermisoAunqueHayaCupo() {
        var decision = politica.evaluar(TipoBloque.DESCANSO_CORTO, 2, false);
        assertFalse(decision.permitida());
        var error = assertThrows(SecurityException.class,
                () -> politica.exigirAceptacion(TipoBloque.DESCANSO_CORTO, 2, false));
        assertEquals(decision.mensaje(), error.getMessage());
    }

    @Test public void antesDelInicioYDuranteAmbosDescansosSePermiteAceptar() {
        for (TipoBloque bloque : new TipoBloque[] {
                null, TipoBloque.DESCANSO_CORTO, TipoBloque.DESCANSO_LARGO}) {
            assertTrue(politica.evaluar(bloque, 3, true).permitida());
            assertEquals("", politica.evaluar(bloque, 3, true).mensaje());
            politica.exigirAceptacion(bloque, 3, true);
        }
    }

    @Test public void unaOcupacionInvalidaNoAutorizaIngresos() {
        for (int ocupacion : new int[] {-1, 0, 5}) {
            assertFalse(politica.evaluar(null, ocupacion, true).permitida());
            assertThrows(IllegalStateException.class,
                    () -> politica.exigirAceptacion(null, ocupacion, true));
        }
    }
}
