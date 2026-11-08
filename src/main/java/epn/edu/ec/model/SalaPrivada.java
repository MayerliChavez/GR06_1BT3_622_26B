package epn.edu.ec.model;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "sala_privada")
public class SalaPrivada {

    public enum Estado {
        ABIERTA,
        CERRADA
    }

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID responsableId;

    @Column(nullable = false, length = 6)
    private String codigo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Estado estado;

    @Enumerated(EnumType.STRING)
    private TipoBloque bloqueActual;

    @Embedded
    private CicloSalaPrivada ciclo = new CicloSalaPrivada();

    protected SalaPrivada() {
        // Requerido por JPA.
    }

    public SalaPrivada(UUID responsableId, String codigo) {
        if (responsableId == null) {
            throw new IllegalArgumentException(
                    "La sala debe tener un responsable."
            );
        }

        if (codigo == null || !codigo.matches("[A-Z0-9]{6}")) {
            throw new IllegalArgumentException(
                    "El código debe tener seis caracteres alfanuméricos."
            );
        }

        this.id = UUID.randomUUID();
        this.responsableId = responsableId;
        this.codigo = codigo;
        this.estado = Estado.ABIERTA;
    }

    public UUID getId() {
        return id;
    }

    public UUID getResponsableId() {
        return responsableId;
    }

    public String getCodigo() {
        return codigo;
    }

    public Estado getEstado() {
        return estado;
    }

    public TipoBloque getBloqueActual() { return bloqueActual; }

    public CicloSalaPrivada getCiclo() {
        if (ciclo == null) {
            ciclo = new CicloSalaPrivada();
        }
        return ciclo;
    }

    public void actualizarBloqueActual(TipoBloque bloque) {
        if (estado != Estado.ABIERTA) {
            throw new IllegalStateException("La sala está cerrada.");
        }

        if (getCiclo().isIniciado()
                && bloque != getCiclo().getTipo()) {
            throw new IllegalStateException(
                    "El bloque debe coincidir con el ciclo común."
            );
        }

        bloqueActual = bloque;
    }
}
