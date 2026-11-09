package epn.edu.ec.salas;

import epn.edu.ec.repository.TransaccionesJpa;
import jakarta.persistence.*;
import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import org.mockito.InOrder;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class TransaccionesJpaMockTest extends PruebaSalaPrivada {

    private EntityManagerFactory factory;
    private EntityManager em;
    private EntityTransaction tx;

    @Before
    public void setUp() {
        factory = mock(EntityManagerFactory.class);
        em = mock(EntityManager.class);
        tx = mock(EntityTransaction.class);

        when(factory.createEntityManager()).thenReturn(em);
        when(em.getTransaction()).thenReturn(tx);
        when(tx.isActive()).thenReturn(true);
    }

    @Test
    public void given_accion_exitosa_when_ejecutar_transaccion_then_confirma_y_cierra_contexto() {
        String resultado = TransaccionesJpa.ejecutar(
                factory,
                contexto -> {
                    assertSame(em, contexto);
                    return "guardado";
                }
        );

        assertEquals("guardado", resultado);

        InOrder orden = inOrder(tx, em);
        orden.verify(tx).begin();
        orden.verify(tx).commit();
        orden.verify(em).close();

        verify(tx, never()).rollback();
    }

    @Test
    public void given_accion_fallida_when_ejecutar_transaccion_then_revierte_y_conserva_error() {
        var fallo = new IllegalStateException(
                "No se pudo guardar."
        );

        var recibido = assertThrows(
                IllegalStateException.class,
                () -> TransaccionesJpa.ejecutar(
                        factory,
                        contexto -> {
                            throw fallo;
                        }
                )
        );

        assertSame(fallo, recibido);

        InOrder orden = inOrder(tx, em);
        orden.verify(tx).begin();
        orden.verify(tx).rollback();
        orden.verify(em).close();

        verify(tx, never()).commit();
    }

    @Test
    public void given_commit_fallido_when_ejecutar_transaccion_then_revierte_y_cierra_contexto() {
        var fallo = new IllegalStateException(
                "Falló el commit."
        );

        doThrow(fallo).when(tx).commit();

        var recibido = assertThrows(
                IllegalStateException.class,
                () -> TransaccionesJpa.ejecutar(
                        factory,
                        contexto -> "resultado"
                )
        );

        assertSame(fallo, recibido);

        InOrder orden = inOrder(tx, em);
        orden.verify(tx).begin();
        orden.verify(tx).commit();
        orden.verify(tx).rollback();
        orden.verify(em).close();
    }

    @After
    public void tearDown() {
        validateMockitoUsage();
        tx = null;
        em = null;
        factory = null;
    }
}
