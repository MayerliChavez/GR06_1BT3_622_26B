package epn.edu.ec.service;

import java.security.SecureRandom;

public class GeneradorCodigoSalaAleatorio implements GeneradorCodigoSala {
    private static final String ALFABETO = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private final SecureRandom random = new SecureRandom();

    @Override public String generar() {
        var codigo = new StringBuilder(6);
        for (int i = 0; i < 6; i++) codigo.append(ALFABETO.charAt(random.nextInt(ALFABETO.length())));
        return codigo.toString();
    }
}
