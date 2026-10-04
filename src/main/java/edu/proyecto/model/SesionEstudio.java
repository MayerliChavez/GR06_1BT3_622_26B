package edu.proyecto.model;

import jakarta.persistence.*;
import java.time.*;
import java.util.*;

@Entity
@Table(name = "sesion_estudio")
public class SesionEstudio {
    @Id private UUID id;
    @Column(nullable = false) private Instant fechaInicio;
    private Instant fechaFin;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private EstadoSesion estado;
    // Campos que materializan las tres relaciones del diagrama.
    @ManyToOne(optional = false, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "estudiante_id", nullable = false) private Estudiante estudiante;
    @Embedded private ConfiguracionPomodoro configuracion;
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "sesion_id", nullable = false)
    @OrderBy("orden ASC") private List<BloquePomodoro> bloques = new ArrayList<>();

    protected SesionEstudio() {} // Constructor técnico requerido por JPA.

    public SesionEstudio(Estudiante estudiante, ConfiguracionPomodoro configuracion, Instant ahora) {
        this.id = UUID.randomUUID();
        this.estudiante = Objects.requireNonNull(estudiante);
        this.configuracion = Objects.requireNonNull(configuracion);
        this.fechaInicio = Objects.requireNonNull(ahora);
        this.estado = EstadoSesion.ACTIVA;
    }

    public BloquePomodoro iniciarSiguienteBloque(Instant ahora) {
        exigirActiva(ahora);
        TipoBloque tipo = TipoBloque.CONCENTRACION;
        if (!bloques.isEmpty()) {
            BloquePomodoro anterior = bloqueActual();
            if (anterior.getEstado() != EstadoBloque.COMPLETADO)
                throw new IllegalStateException("Primero debe completarse el bloque actual.");
            if (anterior.getTipo() == TipoBloque.CONCENTRACION) {
                int completadas = (int) bloques.stream().filter(b -> b.getTipo() == TipoBloque.CONCENTRACION
                        && b.getEstado() == EstadoBloque.COMPLETADO).count();
                tipo = configuracion.tipoDescansoTras(completadas);
            }
        }
        BloquePomodoro bloque = new BloquePomodoro(bloques.size() + 1, tipo, configuracion.duracionPara(tipo), ahora);
        bloques.add(bloque);
        return bloque;
    }

    public void pausar(Instant ahora) { exigirActiva(ahora); bloqueActual().pausar(ahora); }
    public void reanudar(Instant ahora) { exigirActiva(ahora); bloqueActual().reanudar(ahora); }
    public void completarBloqueActual(Instant ahora) { exigirActiva(ahora); bloqueActual().completar(ahora); }

    public Optional<Duration> tiempoRestanteBloqueActual(Instant ahora) {
        Objects.requireNonNull(ahora);
        if (estado != EstadoSesion.ACTIVA || bloques.isEmpty()) return Optional.empty();
        BloquePomodoro actual = bloqueActual();
        if (actual.getEstado() == EstadoBloque.COMPLETADO || actual.getEstado() == EstadoBloque.CANCELADO)
            return Optional.empty();
        return Optional.of(actual.tiempoRestante(ahora));
    }

    public void finalizar(Instant ahora) {
        exigirActiva(ahora);
        if (!bloques.isEmpty()) {
            BloquePomodoro actual = bloqueActual();
            if (actual.getEstado() == EstadoBloque.EN_EJECUCION && actual.tiempoRestante(ahora).isZero())
                actual.completar(ahora);
            else if (actual.getEstado() == EstadoBloque.EN_EJECUCION || actual.getEstado() == EstadoBloque.PAUSADO)
                actual.cancelar(ahora);
        }
        estado = EstadoSesion.FINALIZADA;
        fechaFin = ahora;
    }

    public void abandonar(Instant ahora) {
        exigirActiva(ahora);
        if (!bloques.isEmpty()) {
            BloquePomodoro actual = bloqueActual();
            if (actual.getEstado() == EstadoBloque.EN_EJECUCION || actual.getEstado() == EstadoBloque.PAUSADO)
                actual.cancelar(ahora);
        }
        estado = EstadoSesion.ABANDONADA;
        fechaFin = ahora;
    }

    public Duration tiempoEstudiado(Instant ahora) {
        return bloques.stream().filter(b -> b.getTipo() == TipoBloque.CONCENTRACION)
                .map(b -> b.tiempoActivo(ahora)).reduce(Duration.ZERO, Duration::plus);
    }

    private BloquePomodoro bloqueActual() {
        if (bloques.isEmpty()) throw new IllegalStateException("La sesión no tiene bloques.");
        return bloques.get(bloques.size() - 1);
    }

    private void exigirActiva(Instant ahora) {
        Objects.requireNonNull(ahora);
        if (estado != EstadoSesion.ACTIVA) throw new IllegalStateException("La sesión ya está cerrada.");
        Instant ultimaFecha = bloques.isEmpty() ? fechaInicio : bloqueActual().getFechaFin();
        if (ahora.isBefore(fechaInicio) || (ultimaFecha != null && ahora.isBefore(ultimaFecha)))
            throw new IllegalArgumentException("La fecha no puede retroceder.");
    }

    public UUID getId() { return id; }
    public Instant getFechaInicio() { return fechaInicio; }
    public Instant getFechaFin() { return fechaFin; }
    public EstadoSesion getEstado() { return estado; }
    public Estudiante getEstudiante() { return estudiante; }
    public List<BloquePomodoro> getBloques() { return Collections.unmodifiableList(bloques); }
}
