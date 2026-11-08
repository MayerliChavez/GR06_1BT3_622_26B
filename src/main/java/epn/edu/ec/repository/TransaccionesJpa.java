package epn.edu.ec.repository;

import jakarta.persistence.*;
import java.util.Objects;
import java.util.function.Function;

public final class TransaccionesJpa {

    private TransaccionesJpa() {
    }

    public static <T> T ejecutar(
            EntityManagerFactory factory,
            Function<EntityManager, T> accion
    ) {
        Objects.requireNonNull(factory);
        Objects.requireNonNull(accion);

        try (var em = factory.createEntityManager()) {
            var tx = em.getTransaction();

            try {
                tx.begin();

                T resultado = accion.apply(em);

                tx.commit();
                return resultado;

            } catch (RuntimeException error) {
                if (tx.isActive()) {
                    tx.rollback();
                }

                throw error;
            }
        }
    }
}