package epn.edu.ec.service;

import epn.edu.ec.model.AvisoCierreSalaPrivada;
import epn.edu.ec.repository.TransaccionesJpa;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class ServicioAvisosCierreSala {

    private final EntityManagerFactory factory;

    public ServicioAvisosCierreSala(EntityManagerFactory factory) {
        this.factory = Objects.requireNonNull(factory);
    }

    // Usa la transacción del cierre para que sala y avisos se guarden juntos.
    void registrarEnTransaccion(
            EntityManager em,
            UUID estudianteId,
            UUID responsableId,
            UUID salaId,
            Instant instante
    ) {
        String mensaje = estudianteId.equals(responsableId)
                ? "La sala se cerró correctamente."
                : "El responsable cerró la sala. "
                  + "Tu tiempo de estudio quedó guardado.";

        em.merge(new AvisoCierreSalaPrivada(
                estudianteId,
                salaId,
                mensaje,
                instante
        ));
    }

    public Optional<String> consumirAviso(UUID estudianteId) {
        Objects.requireNonNull(estudianteId);

        return TransaccionesJpa.ejecutar(factory, em -> {
            var aviso = em.find(
                    AvisoCierreSalaPrivada.class,
                    estudianteId,
                    LockModeType.PESSIMISTIC_WRITE
            );

            if (aviso == null) {
                return Optional.empty();
            }

            String mensaje = aviso.getMensaje();
            em.remove(aviso);

            return Optional.of(mensaje);
        });
    }
}
