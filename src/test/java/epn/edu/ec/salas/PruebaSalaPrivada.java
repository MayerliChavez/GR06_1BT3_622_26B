package epn.edu.ec.salas;

import org.junit.AfterClass;
import org.junit.BeforeClass;

/** Ciclo de clase compartido por las pruebas JUnit 4 de salas privadas. */
public abstract class PruebaSalaPrivada {

    @BeforeClass
    public static void setUpClass() {
        System.out.println("setUpClass(): inicio de pruebas de sala privada");
    }

    @AfterClass
    public static void tearDownClass() {
        System.out.println("tearDownClass(): fin de pruebas de sala privada");
    }
}
