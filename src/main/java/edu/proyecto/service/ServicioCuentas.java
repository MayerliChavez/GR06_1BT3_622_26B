package edu.proyecto.service;
import edu.proyecto.model.*;
import jakarta.persistence.*;
import java.util.*;

/** Dos operaciones funcionales adicionales autorizadas: registrar y autenticar. */
public final class ServicioCuentas {
    private final EntityManagerFactory factory;
    private final ProcesadorContrasenas procesadorContrasenas = new ProcesadorContrasenas();
    public ServicioCuentas(EntityManagerFactory factory) { this.factory=Objects.requireNonNull(factory); }

    public synchronized CuentaUsuario registrar(String nombre, String correo, String contrasena) {
        nombre=Objects.toString(nombre, "").strip(); correo=normalizar(correo);
        if (nombre.isEmpty() || nombre.length()>80) throw new IllegalArgumentException("Ingresa un nombre de hasta 80 caracteres.");
        if (correo.length()>254 || !correo.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) throw new
                IllegalArgumentException("Ingresa un correo válido.");
        if (contrasena==null || contrasena.length()<8 || contrasena.length()>128) throw new
                IllegalArgumentException("La contraseña debe tener entre 8 y 128 caracteres.");
        try (var em=factory.createEntityManager()) {
            if (!em.createQuery("select c from CuentaUsuario c where c.correo=:correo", CuentaUsuario.class).setParameter("correo",correo).getResultList().isEmpty())
                throw new IllegalArgumentException("Este correo ya está registrado.");
            String hash = procesadorContrasenas.generarHash(contrasena);
            var cuenta=new CuentaUsuario(new Estudiante(UUID.randomUUID(),nombre),correo,hash);
            var tx=em.getTransaction();
            try { tx.begin(); em.persist(cuenta); tx.commit(); return cuenta; }
            catch (RuntimeException e) { if(tx.isActive())tx.rollback(); throw e; }
        }
    }

    public Optional<CuentaUsuario> autenticar(String correo, String contrasena) {
        if (contrasena==null || contrasena.length()>128) return Optional.empty();
        try (var em=factory.createEntityManager()) {
            var cuentas=em.createQuery("select c from CuentaUsuario c join fetch c.estudiante where c.correo=:correo",CuentaUsuario.class)
                    .setParameter("correo",normalizar(correo)).getResultList();
            if(cuentas.isEmpty())return Optional.empty();
            var cuenta=cuentas.get(0);
            return procesadorContrasenas.verificar(contrasena, cuenta.getHashContrasena())
                    ? Optional.of(cuenta) : Optional.empty();
        }
    }
    private String normalizar(String correo) { return Objects.toString(correo, "").strip().toLowerCase(Locale.ROOT); }
}
