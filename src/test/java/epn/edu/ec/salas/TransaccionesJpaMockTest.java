package epn.edu.ec.salas;

import epn.edu.ec.repository.TransaccionesJpa;
import jakarta.persistence.*;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class TransaccionesJpaMockTest {

    private EntityManagerFactory factory;
    private EntityManager em;
    private EntityTransaction tx;

    @Before
    public void preparar() {
        factory = mock(EntityManagerFactory.class);
        em = mock(EntityManager.class);
        tx = mock(EntityTransaction.class);

        when(factory.createEntityManager()).thenReturn(em);
        when(em.getTransaction()).thenReturn(tx);
        when(tx.isActive()).thenReturn(true);
    }

    @Test
    public void confirmaYLiberaElEntityManagerCuandoLaAccionTermina() {
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
    public void revierteYConservaElErrorCuandoFallaLaAccion() {
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
    public void revierteYLiberaRecursosSiFallaElCommit() {
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
}