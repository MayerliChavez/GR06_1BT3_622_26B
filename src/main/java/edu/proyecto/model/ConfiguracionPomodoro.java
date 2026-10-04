package edu.proyecto.model;

import jakarta.persistence.*;
import java.time.Duration;
import java.util.Objects;

@Embeddable
public class ConfiguracionPomodoro {
    @Column(nullable = false) private Duration duracionConcentracion;
    @Column(nullable = false) private Duration duracionDescansoCorto;
    @Column(nullable = false) private Duration duracionDescansoLargo;
    @Column(nullable = false) private int bloquesAntesDescansoLargo;

    protected ConfiguracionPomodoro() {} // Constructor técnico requerido por JPA.

    public ConfiguracionPomodoro(Duration concentracion, Duration descansoCorto,
                                 Duration descansoLargo, int bloquesAntesDescansoLargo) {
        Objects.requireNonNull(concentracion);
        Objects.requireNonNull(descansoCorto);
        Objects.requireNonNull(descansoLargo);
        if (concentracion.isNegative() || concentracion.isZero() || descansoCorto.isNegative()
                || descansoCorto.isZero() || descansoLargo.isNegative() || descansoLargo.isZero()
                || bloquesAntesDescansoLargo < 1)
            throw new IllegalArgumentException("Las duraciones y el número de bloques deben ser positivos.");
        this.duracionConcentracion = concentracion;
        this.duracionDescansoCorto = descansoCorto;
        this.duracionDescansoLargo = descansoLargo;
        this.bloquesAntesDescansoLargo = bloquesAntesDescansoLargo;
    }

    public Duration duracionPara(TipoBloque tipo) {
        return switch (Objects.requireNonNull(tipo)) {
            case CONCENTRACION -> duracionConcentracion;
            case DESCANSO_CORTO -> duracionDescansoCorto;
            case DESCANSO_LARGO -> duracionDescansoLargo;
        };
    }

    public TipoBloque tipoDescansoTras(int concentracionesCompletadas) {
        if (concentracionesCompletadas < 1)
            throw new IllegalArgumentException("Debe existir una concentración completada.");
        return concentracionesCompletadas % bloquesAntesDescansoLargo == 0
                ? TipoBloque.DESCANSO_LARGO : TipoBloque.DESCANSO_CORTO;
    }
}
