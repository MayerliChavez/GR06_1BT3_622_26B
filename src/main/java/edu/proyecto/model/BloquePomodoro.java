package edu.proyecto.model;

import jakarta.persistence.*;
import java.time.*;
import java.util.*;

@Entity
@Table(name = "bloque_pomodoro")
public class BloquePomodoro {
    @Id private UUID id;
    @Column(nullable = false) private int orden;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private TipoBloque tipo;
    @Column(nullable = false) private Duration duracionObjetivo;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private EstadoBloque estado;
    @Column(nullable = false) private Instant fechaInicio;
    private Instant fechaFin;
    @Column(nullable = false) private Duration tiempoActivoAcumulado;
    private Instant inicioTramoActual;

    protected BloquePomodoro() {} // Constructor técnico requerido por JPA.

    public BloquePomodoro(int orden, TipoBloque tipo, Duration duracionObjetivo, Instant ahora) {
        if (orden < 1) throw new IllegalArgumentException("El orden empieza en 1.");
        Objects.requireNonNull(duracionObjetivo);
        if (duracionObjetivo.isZero() || duracionObjetivo.isNegative())
            throw new IllegalArgumentException("La duración debe ser positiva.");
        this.id = UUID.randomUUID();
        this.orden = orden;
        this.tipo = Objects.requireNonNull(tipo);
        this.duracionObjetivo = duracionObjetivo;
        this.fechaInicio = Objects.requireNonNull(ahora);
        this.inicioTramoActual = ahora;
        this.tiempoActivoAcumulado = Duration.ZERO;
        this.estado = EstadoBloque.EN_EJECUCION;
    }

    public void pausar(Instant ahora) {
        if (estado != EstadoBloque.EN_EJECUCION)
            throw new IllegalStateException("Solo se puede pausar un bloque en ejecución.");
        if (tiempoRestante(ahora).isZero())
            throw new IllegalStateException("El bloque ya terminó; debe completarse.");
        tiempoActivoAcumulado = tiempoActivo(ahora);
        inicioTramoActual = null;
        estado = EstadoBloque.PAUSADO;
    }

    public void reanudar(Instant ahora) {
        validarAhora(ahora);
        if (estado != EstadoBloque.PAUSADO)
            throw new IllegalStateException("Solo se puede reanudar un bloque pausado.");
        inicioTramoActual = ahora;
        estado = EstadoBloque.EN_EJECUCION;
    }

    public void completar(Instant ahora) {
        if (estado != EstadoBloque.EN_EJECUCION || !tiempoRestante(ahora).isZero())
            throw new IllegalStateException("El bloque debe agotar su tiempo antes de completarse.");
        // La fecha real de vencimiento evita contar tiempo extra entre consultas.
        fechaFin = inicioTramoActual.plus(duracionObjetivo.minus(tiempoActivoAcumulado));
        tiempoActivoAcumulado = duracionObjetivo;
        inicioTramoActual = null;
        estado = EstadoBloque.COMPLETADO;
    }

    public void cancelar(Instant ahora) {
        validarAhora(ahora);
        if (estado != EstadoBloque.EN_EJECUCION && estado != EstadoBloque.PAUSADO)
            throw new IllegalStateException("No se puede cancelar un bloque cerrado.");
        tiempoActivoAcumulado = tiempoActivo(ahora);
        inicioTramoActual = null;
        fechaFin = ahora;
        estado = EstadoBloque.CANCELADO;
    }

    public Duration tiempoActivo(Instant ahora) {
        validarAhora(ahora);
        Duration total = tiempoActivoAcumulado;
        if (estado == EstadoBloque.EN_EJECUCION)
            total = total.plus(Duration.between(inicioTramoActual, ahora));
        return total.compareTo(duracionObjetivo) > 0 ? duracionObjetivo : total;
    }

    public Duration tiempoRestante(Instant ahora) {
        return duracionObjetivo.minus(tiempoActivo(ahora));
    }

    private void validarAhora(Instant ahora) {
        Objects.requireNonNull(ahora);
        if (ahora.isBefore(fechaInicio) || (inicioTramoActual != null && ahora.isBefore(inicioTramoActual)))
            throw new IllegalArgumentException("La fecha no puede retroceder.");
    }

    public UUID getId() { return id; }
    public int getOrden() { return orden; }
    public TipoBloque getTipo() { return tipo; }
    public Duration getDuracionObjetivo() { return duracionObjetivo; }
    public EstadoBloque getEstado() { return estado; }
    public Instant getFechaInicio() { return fechaInicio; }
    public Instant getFechaFin() { return fechaFin; }
}
