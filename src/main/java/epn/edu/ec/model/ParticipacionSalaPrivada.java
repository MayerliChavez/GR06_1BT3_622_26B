package epn.edu.ec.model;

import jakarta.persistence.*;
import java.util.Objects;
import java.util.UUID;

/** Una fila por estudiante mientras pertenece a una sala abierta. */
@Entity
@Table(name = "participacion_sala_privada")
public class ParticipacionSalaPrivada {
    @Id private UUID estudianteId;
    @ManyToOne(optional = false)
    @JoinColumn(name = "sala_id", nullable = false)
    private SalaPrivada sala;
    // Solo el responsable reserva el código; los demás participantes usan null.
    @Column(name = "codigo_reservado", length = 6, unique = true)
    private String codigoReservado;

    protected ParticipacionSalaPrivada() {}

    public ParticipacionSalaPrivada(UUID estudianteId, SalaPrivada sala) {
        this.estudianteId = Objects.requireNonNull(estudianteId);
        this.sala = Objects.requireNonNull(sala);
        if (estudianteId.equals(sala.getResponsableId())) codigoReservado = sala.getCodigo();
    }

    public UUID getEstudianteId() { return estudianteId; }
    public SalaPrivada getSala() { return sala; }
}
