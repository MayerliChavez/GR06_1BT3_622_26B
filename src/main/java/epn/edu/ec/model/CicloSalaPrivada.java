package epn.edu.ec.model;

import jakarta.persistence.*;
import java.time.*;
import java.util.Objects;

@Embeddable
public class CicloSalaPrivada {
    @Enumerated(EnumType.STRING)
    @Column(name = "ciclo_tipo")
    private TipoBloque tipo;

    @Column(name = "ciclo_inicio_bloque")
    private Instant inicioBloque;

    @Column(name = "ciclo_fin_bloque")
    private Instant finBloque;

    @Column(name = "ciclo_concentraciones")
    private Integer concentraciones = 0;

    @Column(name = "ciclo_orden")
    private Integer orden = 0;

    public void iniciar(Instant ahora) {
        Objects.requireNonNull(ahora);

        if (isIniciado()) {
            throw new IllegalStateException(
                    "El ciclo ya está iniciado."
            );
        }

        tipo = TipoBloque.CONCENTRACION;
        inicioBloque = ahora;
        finBloque = ahora.plusSeconds(1500);
        concentraciones = 0;
        orden = 1;
    }

    public boolean avanzarSiCorresponde(Instant ahora) {
        Objects.requireNonNull(ahora);

        if (!isIniciado()) {
            return false;
        }
        if (ahora.isBefore(inicioBloque)) {
            throw new IllegalArgumentException(
                    "La fecha no puede retroceder."
            );
        }
        if (ahora.isBefore(finBloque)) {
            return false;
        }

        if (tipo == TipoBloque.CONCENTRACION) {
            concentraciones =
                    (concentraciones == null ? 0 : concentraciones) + 1;

            tipo = concentraciones % 4 == 0
                    ? TipoBloque.DESCANSO_LARGO
                    : TipoBloque.DESCANSO_CORTO;
        } else {
            tipo = TipoBloque.CONCENTRACION;
        }

        inicioBloque = finBloque;

        long segundos = tipo == TipoBloque.CONCENTRACION
                ? 1500
                : tipo == TipoBloque.DESCANSO_CORTO ? 300 : 900;

        finBloque = inicioBloque.plusSeconds(segundos);
        orden = getOrden() + 1;

        return true;
    }

    public Duration tiempoRestante(Instant ahora) {
        Objects.requireNonNull(ahora);

        if (!isIniciado() || !ahora.isBefore(finBloque)) {
            return Duration.ZERO;
        }
        if (ahora.isBefore(inicioBloque)) {
            throw new IllegalArgumentException(
                    "La fecha no puede retroceder."
            );
        }

        return Duration.between(ahora, finBloque);
    }

    public boolean isIniciado() {
        return inicioBloque != null;
    }

    public TipoBloque getTipo() {
        return tipo;
    }

    public Instant getInicioBloque() {
        return inicioBloque;
    }

    public Instant getFinBloque() {
        return finBloque;
    }

    public int getOrden() {
        return orden == null ? 0 : orden;
    }
}