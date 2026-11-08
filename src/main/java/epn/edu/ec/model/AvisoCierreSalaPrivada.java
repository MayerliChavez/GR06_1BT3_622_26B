package epn.edu.ec.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "aviso_cierre_sala_privada")
public class AvisoCierreSalaPrivada {

    @Id
    private UUID estudianteId;

    @Column(nullable = false)
    private UUID salaId;

    @Column(nullable = false, length = 1000)
    private String mensaje;

    @Column(nullable = false)
    private Instant fecha;

    protected AvisoCierreSalaPrivada() {
    }

    public AvisoCierreSalaPrivada(
            UUID estudianteId,
            UUID salaId,
            String mensaje,
            Instant fecha
    ) {
        this.estudianteId = Objects.requireNonNull(estudianteId);
        this.salaId = Objects.requireNonNull(salaId);
        this.mensaje = Objects.requireNonNull(mensaje);
        this.fecha = Objects.requireNonNull(fecha);
    }

    public String getMensaje() {
        return mensaje;
    }
}