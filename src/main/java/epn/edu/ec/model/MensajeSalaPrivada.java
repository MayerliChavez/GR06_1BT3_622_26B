package epn.edu.ec.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(
        name = "mensaje_sala_privada",
        indexes = @Index(
                name = "idx_mensaje_sala_privada",
                columnList = "sala_id,id"
        )
)
public class MensajeSalaPrivada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 2000)
    private String texto;

    @Column(nullable = false)
    private Instant fecha;

    @Column(nullable = false, length = 100)
    private String nombreAutor;

    @ManyToOne(optional = false)
    @JoinColumn(name = "autor_id", nullable = false)
    private Estudiante autor;

    @ManyToOne(optional = false)
    @JoinColumn(name = "sala_id", nullable = false)
    private SalaPrivada sala;

    protected MensajeSalaPrivada() {
        // Requerido por JPA.
    }

    public MensajeSalaPrivada(
            String texto,
            Estudiante autor,
            SalaPrivada sala,
            Instant fecha) {

        this.texto = Objects.requireNonNull(texto);
        this.autor = Objects.requireNonNull(autor);
        this.sala = Objects.requireNonNull(sala);
        this.fecha = Objects.requireNonNull(fecha);
        this.nombreAutor = autor.getNombreVisible();
    }

    public Long getId() {
        return id;
    }

    public String getTexto() {
        return texto;
    }

    public Instant getFecha() {
        return fecha;
    }

    public String getNombreAutor() {
        return nombreAutor;
    }

    public UUID getAutorId() {
        return autor.getId();
    }

    public UUID getSalaId() {
        return sala.getId();
    }
}