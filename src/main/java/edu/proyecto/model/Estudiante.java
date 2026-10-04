package edu.proyecto.model;

import jakarta.persistence.*;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "estudiante")
public class Estudiante {
    @Id private UUID id;
    @Column(nullable = false, length = 100) private String nombreVisible;

    protected Estudiante() {} // Constructor técnico requerido por JPA.

    public Estudiante(UUID id, String nombreVisible) {
        this.id = Objects.requireNonNull(id, "El estudiante necesita un id.");
        cambiarNombreVisible(nombreVisible);
    }

    public void cambiarNombreVisible(String nuevoNombre) {
        if (nuevoNombre == null || nuevoNombre.isBlank() || nuevoNombre.trim().length() > 100)
            throw new IllegalArgumentException("El nombre debe tener entre 1 y 100 caracteres.");
        nombreVisible = nuevoNombre.trim();
    }

    public UUID getId() { return id; }
    public String getNombreVisible() { return nombreVisible; }
}
