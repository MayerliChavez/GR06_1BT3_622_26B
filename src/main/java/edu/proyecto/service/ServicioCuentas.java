package edu.proyecto.service;
import edu.proyecto.model.*;
import jakarta.persistence.*;
import java.util.*;
import java.security.*;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** Dos operaciones funcionales adicionales autorizadas: registrar y autenticar. */
public final class ServicioCuentas {
    private final EntityManagerFactory factory;
    public ServicioCuentas(EntityManagerFactory factory) { this.factory=Objects.requireNonNull(factory); }
    public synchronized CuentaUsuario registrar(String nombre, String correo, String contrasena) {
        nombre=Objects.toString(nombre, "").strip(); correo=normalizar(correo);
        if (nombre.isEmpty() || nombre.length()>80) throw new IllegalArgumentException("Ingresa un nombre de hasta 80 caracteres.");
        if (correo.length()>254 || !correo.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) throw new IllegalArgumentException("Ingresa un correo válido.");
        if (contrasena==null || contrasena.length()<8 || contrasena.length()>128) throw new IllegalArgumentException("La contraseña debe tener entre 8 y 128 caracteres.");
        try (var em=factory.createEntityManager()) {
            if (!em.createQuery("select c from CuentaUsuario c where c.correo=:correo", CuentaUsuario.class).setParameter("correo",correo).getResultList().isEmpty())
                throw new IllegalArgumentException("Este correo ya está registrado.");
            byte[] sal=new byte[16]; new SecureRandom().nextBytes(sal);
            String hash="600000:"+Base64.getEncoder().encodeToString(sal)+":"+Base64.getEncoder().encodeToString(derivar(contrasena,sal));
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
            var cuenta=cuentas.get(0); var partes=cuenta.getHashContrasena().split(":");
            return MessageDigest.isEqual(Base64.getDecoder().decode(partes[2]),derivar(contrasena,Base64.getDecoder().decode(partes[1]))) ? Optional.of(cuenta) : Optional.empty();
        }
    }
    private String normalizar(String correo) { return Objects.toString(correo, "").strip().toLowerCase(Locale.ROOT); }
    private byte[] derivar(String contrasena, byte[] sal) {
        var spec=new PBEKeySpec(contrasena.toCharArray(),sal,600000,256);
        try { return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded(); }
        catch(GeneralSecurityException e) { throw new IllegalStateException("No se pudo procesar la contraseña.",e); }
        finally { spec.clearPassword(); }
    }
}
