package edu.proyecto.service;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** Genera hashes de contraseñas y verifica contraseñas contra hashes guardados. */
public final class ProcesadorContrasenas {
    public String generarHash(String contrasena) {
        byte[] sal = new byte[16];
        new SecureRandom().nextBytes(sal);
        return "600000:" + Base64.getEncoder().encodeToString(sal) + ":"
                + Base64.getEncoder().encodeToString(derivar(contrasena, sal));
    }

    public boolean verificar(String contrasena, String hashGuardado) {
        var partes = hashGuardado.split(":");
        return MessageDigest.isEqual(Base64.getDecoder().decode(partes[2]),
                derivar(contrasena, Base64.getDecoder().decode(partes[1])));
    }

    private byte[] derivar(String contrasena, byte[] sal) {
        var spec = new PBEKeySpec(contrasena.toCharArray(), sal, 600000, 256);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo procesar la contraseña.", e);
        } finally {
            spec.clearPassword();
        }
    }
}
