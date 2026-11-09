package epn.edu.ec.salas;

import epn.edu.ec.model.*;
import java.time.*;
import org.junit.Test;
import org.junit.Before;
import org.junit.After;

import static org.junit.Assert.*;

public class CicloSalaPrivadaTest extends PruebaSalaPrivada {
    private CicloSalaPrivada ciclo;
    private Instant inicio;

    @Before
    public void setUp() {
        inicio = Instant.parse("2026-11-08T15:00:00Z");
        ciclo = new CicloSalaPrivada();
    }

    @Test
    public void given_ciclo_sin_iniciar_when_iniciar_then_concentracion_de_25_minutos() {

        ciclo.iniciar(inicio);

        assertEquals(TipoBloque.CONCENTRACION, ciclo.getTipo());
        assertEquals(inicio.plusSeconds(1500), ciclo.getFinBloque());
        assertEquals(
                Duration.ofMinutes(25),
                ciclo.tiempoRestante(inicio)
        );
    }

    @Test
    public void given_concentracion_when_vencer_bloque_then_descanso_sin_transicion_duplicada() {
        ciclo.iniciar(inicio);

        assertFalse(
                ciclo.avanzarSiCorresponde(inicio.plusSeconds(1499))
        );
        assertTrue(
                ciclo.avanzarSiCorresponde(inicio.plusSeconds(1500))
        );
        assertEquals(TipoBloque.DESCANSO_CORTO, ciclo.getTipo());
        assertEquals(inicio.plusSeconds(1800), ciclo.getFinBloque());

        assertFalse(
                ciclo.avanzarSiCorresponde(inicio.plusSeconds(1500))
        );
        assertEquals(2, ciclo.getOrden());
    }

    @Test
    public void given_cuarta_concentracion_when_finalizar_then_descanso_largo_de_15_minutos() {
        ciclo.iniciar(inicio);

        for (int i = 1; i <= 4; i++) {
            Instant fin = ciclo.getFinBloque();

            assertTrue(ciclo.avanzarSiCorresponde(fin));

            assertEquals(
                    i == 4
                            ? TipoBloque.DESCANSO_LARGO
                            : TipoBloque.DESCANSO_CORTO,
                    ciclo.getTipo()
            );
            assertEquals(
                    Duration.ofMinutes(i == 4 ? 15 : 5),
                    Duration.between(fin, ciclo.getFinBloque())
            );

            if (i < 4) {
                ciclo.avanzarSiCorresponde(ciclo.getFinBloque());
            }
        }
    }

    @Test
    public void given_ciclo_iniciado_when_reiniciar_then_rechaza_y_conserva_inicio() {
        ciclo.iniciar(inicio);

        assertThrows(
                IllegalStateException.class,
                () -> ciclo.iniciar(inicio.plusSeconds(60))
        );
        assertEquals(inicio, ciclo.getInicioBloque());
    }

    @After
    public void tearDown() {
        ciclo = null;
        inicio = null;
    }
}
