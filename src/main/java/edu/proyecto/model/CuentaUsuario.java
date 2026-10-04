package edu.proyecto.model;
import jakarta.persistence.*;
import java.util.UUID;

/** Ampliación autorizada para CU01 y CU02 del incremento 2. */
@Entity @Table(name="cuenta_usuario")
public class CuentaUsuario {
    @Id private UUID id;
    @Column(nullable=false, unique=true, length=254) private String correo;
    @Column(nullable=false, length=200) private String hashContrasena;
    @OneToOne(optional=false, cascade=CascadeType.PERSIST) @JoinColumn(name="estudiante_id", nullable=false, unique=true)
    private Estudiante estudiante;
    protected CuentaUsuario() {}
    public CuentaUsuario(Estudiante estudiante, String correo, String hashContrasena) {
        this.id=estudiante.getId(); this.estudiante=estudiante; this.correo=correo; this.hashContrasena=hashContrasena;
    }
    public UUID getId() { return id; }
    public String getCorreo() { return correo; }
    public String getHashContrasena() { return hashContrasena; }
    public Estudiante getEstudiante() { return estudiante; }
}
