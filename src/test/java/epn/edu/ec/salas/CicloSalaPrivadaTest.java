package epn.edu.ec.salas;

import epn.edu.ec.model.*;
import java.time.*;
import org.junit.Test;

import static org.junit.Assert.*;

public class CicloSalaPrivadaTest {
    private final Instant inicio =
            Instant.parse("2026-11-08T15:00:00Z");

    @Test
    public void inicioFijaConcentracionDe25Minutos() {
        var ciclo = new CicloSalaPrivada();

        ciclo.iniciar(inicio);

        assertEquals(TipoBloque.CONCENTRACION, ciclo.getTipo());
        assertEquals(inicio.plusSeconds(1500), ciclo.getFinBloque());
        assertEquals(
                Duration.ofMinutes(25),
                ciclo.tiempoRestante(inicio)
        );
    }

    @Test
    public void vencimientoIniciaDescansoSinDuplicarLaTransicion() {
        var ciclo = new CicloSalaPrivada();
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
    public void cuartaConcentracionDaDescansoLargoDe15Minutos() {
        var ciclo = new CicloSalaPrivada();
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
    public void unCicloIniciadoNoSeReinicia() {
        var ciclo = new CicloSalaPrivada();
        ciclo.iniciar(inicio);

        assertThrows(
                IllegalStateException.class,
                () -> ciclo.iniciar(inicio.plusSeconds(60))
        );
        assertEquals(inicio, ciclo.getInicioBloque());
    }
}