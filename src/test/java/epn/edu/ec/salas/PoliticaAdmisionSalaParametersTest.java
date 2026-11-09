package epn.edu.ec.salas;

import epn.edu.ec.model.TipoBloque;
import epn.edu.ec.service.PoliticaAdmisionSala;
import java.util.Arrays;
import java.util.Collection;
import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class PoliticaAdmisionSalaParametersTest extends PruebaSalaPrivada {

    private PoliticaAdmisionSala politica;

    private final TipoBloque bloque;
    private final int participantes;
    private final boolean responsable;
    private final boolean esperado;

    @Parameterized.Parameters(
            name = "{index}: bloque={0}, participantes={1}, responsable={2}"
    )
    public static Collection<Object[]> casos() {
        return Arrays.asList(new Object[][] {
                // null significa que el estudio todavía no inició.
                {null, 1, true, true},
                {null, 2, true, true},
                {null, 3, true, true},
                {null, 4, true, false},

                // Durante concentración no se acepta.
                {TipoBloque.CONCENTRACION, 1, true, false},
                {TipoBloque.CONCENTRACION, 2, true, false},
                {TipoBloque.CONCENTRACION, 3, true, false},

                // Descanso corto: aceptar únicamente con cupo.
                {TipoBloque.DESCANSO_CORTO, 1, true, true},
                {TipoBloque.DESCANSO_CORTO, 2, true, true},
                {TipoBloque.DESCANSO_CORTO, 3, true, true},
                {TipoBloque.DESCANSO_CORTO, 4, true, false},

                // La misma regla durante el descanso largo.
                {TipoBloque.DESCANSO_LARGO, 1, true, true},
                {TipoBloque.DESCANSO_LARGO, 2, true, true},
                {TipoBloque.DESCANSO_LARGO, 3, true, true},
                {TipoBloque.DESCANSO_LARGO, 4, true, false},

                // Un invitado no puede aceptar solicitudes.
                {null, 2, false, false},
                {TipoBloque.DESCANSO_CORTO, 2, false, false},
                {TipoBloque.DESCANSO_LARGO, 2, false, false}
        });
    }

    public PoliticaAdmisionSalaParametersTest(
            TipoBloque bloque,
            int participantes,
            boolean responsable,
            boolean esperado) {
        this.bloque = bloque;
        this.participantes = participantes;
        this.responsable = responsable;
        this.esperado = esperado;
    }

    @Before
    public void setUp() {
        politica = new PoliticaAdmisionSala();
    }

    @Test
    public void given_fase_cupo_y_rol_when_evaluar_admision_then_coincide_con_resultado_esperado() {

        boolean resultado = politica.puedeAceptar(
                bloque,
                participantes,
                responsable
        );

        assertEquals(esperado, resultado);
    }

    @After
    public void tearDown() {
        politica = null;
    }
}
