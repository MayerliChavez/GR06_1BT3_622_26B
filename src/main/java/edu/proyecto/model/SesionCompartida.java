package edu.proyecto.model;

import jakarta.persistence.*;
import java.time.*;
import java.util.*;

@Entity
@Table(name = "sesion_compartida")
public class SesionCompartida {
    @Id private UUID id;
    @Column(nullable = false) private Instant fechaInicio;
    private Instant fechaFin;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private EstadoSesionCompartida estado;
    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "compartida_id")
    private List<SesionEstudio> sesiones = new ArrayList<>();
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "compartida_id", nullable = false)
    private List<ParticipacionCompartida> participaciones = new ArrayList<>();

    protected SesionCompartida() {}
    public SesionCompartida(SesionEstudio primera, SesionEstudio segunda) {
        Objects.requireNonNull(primera); Objects.requireNonNull(segunda);
        if (primera.getEstudiante().getId().equals(segunda.getEstudiante().getId()))
            throw new IllegalArgumentException("Se necesitan dos estudiantes distintos.");
        id = UUID.randomUUID();
        estado = EstadoSesionCompartida.ACTIVA;
        sesiones.add(primera); sesiones.add(segunda);
        participaciones.add(new ParticipacionCompartida(primera));
        participaciones.add(new ParticipacionCompartida(segunda));
    }

    public void iniciar(Instant ahora) {
        Objects.requireNonNull(ahora);
        if (fechaInicio != null) throw new IllegalStateException("La sesión compartida ya fue iniciada.");
        fechaInicio = ahora;
        iniciarBloquesSiguientes(ahora);
    }

    public void avanzarSiCorresponde(Instant ahora) {
        Objects.requireNonNull(ahora);
        if (fechaInicio == null || ahora.isBefore(fechaInicio))
            throw new IllegalArgumentException("La sesión debe estar iniciada y la fecha no puede retroceder.");
        if (estado != EstadoSesionCompartida.ACTIVA) return;
        while (tiempoRestanteBloqueActual(ahora).map(Duration::isZero).orElse(false)) {
            Instant vencimiento = null;
            for (var p : participaciones) {
                if (!p.estaPresente()) continue;
                var s = p.getSesionEstudio();
                s.completarBloqueActual(ahora);
                Instant fin = s.getBloques().get(s.getBloques().size() - 1).getFechaFin();
                if (vencimiento == null) vencimiento = fin;
                else if (!vencimiento.equals(fin)) throw new IllegalStateException("Los bloques compartidos no están sincronizados.");
            }
            iniciarBloquesSiguientes(vencimiento);
        }
    }

    public void registrarSalida(UUID estudianteId, MotivoSalida motivo, Instant ahora) {
        var participante = participaciones.stream().filter(p -> p.getEstudiante().getId().equals(estudianteId))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("El estudiante no pertenece a esta sesión."));
        if (!participante.estaPresente()) return;
        Objects.requireNonNull(motivo);
        avanzarSiCorresponde(ahora);
        if (motivo == MotivoSalida.VOLUNTARIA) participante.getSesionEstudio().finalizar(ahora);
        else participante.getSesionEstudio().abandonar(ahora);
        participante.registrarSalida(motivo, ahora);
        if (participaciones.stream().noneMatch(ParticipacionCompartida::estaPresente)) finalizar(ahora);
    }

    public Optional<Duration> tiempoRestanteBloqueActual(Instant ahora) {
        if (estado != EstadoSesionCompartida.ACTIVA) return Optional.empty();
        return participaciones.stream().filter(ParticipacionCompartida::estaPresente)
                .findFirst().flatMap(p -> p.getSesionEstudio().tiempoRestanteBloqueActual(ahora));
    }

    // Visibilidad privada, tal como aparece con '-' en el diagrama.
    private void iniciarBloquesSiguientes(Instant ahora) {
        for (var p : participaciones) if (p.estaPresente()) p.getSesionEstudio().iniciarSiguienteBloque(ahora);
    }
    private void finalizar(Instant ahora) {
        fechaFin = ahora;
        estado = participaciones.stream().allMatch(p -> p.getMotivoSalida() == MotivoSalida.DESCONEXION)
                ? EstadoSesionCompartida.ABANDONADA : EstadoSesionCompartida.CERRADA;
    }

    public UUID getId() { return id; }
    public Instant getFechaInicio() { return fechaInicio; }
    public Instant getFechaFin() { return fechaFin; }
    public EstadoSesionCompartida getEstado() { return estado; }
    public List<SesionEstudio> getSesiones() { return Collections.unmodifiableList(sesiones); }
    public List<ParticipacionCompartida> getParticipaciones() { return Collections.unmodifiableList(participaciones); }
}
