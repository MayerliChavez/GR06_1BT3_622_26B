package epn.edu.ec.model;

import epn.edu.ec.tdd.chat.Mensaje;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "mensaje_chat", indexes = @Index(name = "idx_chat_sesion", columnList = "sesion_id,id"))
public class MensajeChat {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, length = 2000) private String texto;
    @Column(nullable = false) private Instant fecha;
    @Column(nullable = false, length = 100) private String nombreAutor;
    @ManyToOne(optional = false) @JoinColumn(name = "autor_id") private Estudiante autor;
    @ManyToOne(optional = false) @JoinColumn(name = "sesion_id") private SesionCompartida sesion;

    protected MensajeChat() {}
    public MensajeChat(Mensaje mensaje, Estudiante autor, SesionCompartida sesion) {
        this.texto = mensaje.texto();
        this.autor = autor;
        this.sesion = sesion;
        this.nombreAutor = autor.getNombreVisible();
        this.fecha = Instant.now();
    }
    public Long getId() { return id; }
    public String getTexto() { return texto; }
    public String getNombreAutor() { return nombreAutor; }
    public Instant getFecha() { return fecha; }
    public UUID getAutorId() { return autor.getId(); }
}
