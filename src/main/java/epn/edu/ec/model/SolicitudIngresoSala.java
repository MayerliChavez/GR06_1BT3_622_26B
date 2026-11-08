package epn.edu.ec.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "solicitud_ingreso_sala")
public class SolicitudIngresoSala {

    public enum Estado {
        PENDIENTE,
        ACEPTADA,
        RECHAZADA,
        CANCELADA
    }

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID estudianteId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "sala_id", nullable = false)
    private SalaPrivada sala;

    @Column(nullable = false)
    private Instant fechaSolicitud;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Estado estado;

    protected SolicitudIngresoSala() {
        // Requerido por JPA.
    }

    public SolicitudIngresoSala(
            UUID estudianteId,
            SalaPrivada sala,
            Instant fechaSolicitud) {

        this.id = UUID.randomUUID();
        this.estudianteId = java.util.Objects.requireNonNull(estudianteId);
        this.sala = java.util.Objects.requireNonNull(sala);
        this.fechaSolicitud = java.util.Objects.requireNonNull(fechaSolicitud);
        this.estado = Estado.PENDIENTE;
    }

    public void cancelar(UUID estudianteId) {
        if (!this.estudianteId.equals(estudianteId)) {
            throw new SecurityException(
                    "Solo el solicitante puede cancelar su solicitud."
            );
        }

        if (estado != Estado.PENDIENTE) {
            throw new IllegalStateException(
                    "La solicitud ya fue atendida."
            );
        }

        estado = Estado.CANCELADA;
    }

    public void aceptar(UUID responsableId) {
        if (!sala.getResponsableId().equals(responsableId)) {
            throw new SecurityException("Solo el responsable puede aceptar solicitudes.");
        }
        if (estado != Estado.PENDIENTE) {
            throw new IllegalStateException("La solicitud ya fue atendida.");
        }
        estado = Estado.ACEPTADA;
    }

    public void rechazar(UUID responsableId) {
        if (!sala.getResponsableId().equals(responsableId)) {
            throw new SecurityException(
                    "Solo el responsable puede rechazar solicitudes."
            );
        }

        if (estado != Estado.PENDIENTE) {
            throw new IllegalStateException(
                    "La solicitud ya fue atendida."
            );
        }

        estado = Estado.RECHAZADA;
    }

    public UUID getId() {
        return id;
    }

    public UUID getEstudianteId() {
        return estudianteId;
    }

    public SalaPrivada getSala() {
        return sala;
    }

    public Instant getFechaSolicitud() {
        return fechaSolicitud;
    }

    public Estado getEstado() {
        return estado;
    }
}
