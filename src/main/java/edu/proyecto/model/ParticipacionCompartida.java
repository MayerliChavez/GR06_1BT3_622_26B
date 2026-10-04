package edu.proyecto.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.*;

@Entity
@Table(name = "participacion_compartida")
public class ParticipacionCompartida {
    @Id private UUID id;
    @Column(nullable = false) private Instant fechaInicio;
    private Instant fechaFin;
    @Enumerated(EnumType.STRING) private MotivoSalida motivoSalida;
    @ManyToOne(optional = false, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private Estudiante estudiante;
    @OneToOne(optional = false, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private SesionEstudio sesionEstudio;

    protected ParticipacionCompartida() {}
    public ParticipacionCompartida(SesionEstudio sesionEstudio) {
        this.id = UUID.randomUUID();
        this.sesionEstudio = Objects.requireNonNull(sesionEstudio);
        this.estudiante = sesionEstudio.getEstudiante();
        this.fechaInicio = sesionEstudio.getFechaInicio();
    }
    public boolean estaPresente() { return fechaFin == null; }
    public void registrarSalida(MotivoSalida motivo, Instant ahora) {
        Objects.requireNonNull(motivo); Objects.requireNonNull(ahora);
        if (!estaPresente()) throw new IllegalStateException("La participación ya está cerrada.");
        if (ahora.isBefore(fechaInicio)) throw new IllegalArgumentException("La fecha no puede retroceder.");
        motivoSalida = motivo;
        fechaFin = ahora;
    }
    public UUID getId() { return id; }
    public Estudiante getEstudiante() { return estudiante; }
    public SesionEstudio getSesionEstudio() { return sesionEstudio; }
    public Instant getFechaInicio() { return fechaInicio; }
    public Instant getFechaFin() { return fechaFin; }
    public MotivoSalida getMotivoSalida() { return motivoSalida; }
}
