package edu.proyecto.repository;
import java.security.SecureRandom;
import java.util.*;

/** Adaptador de la cola de búsqueda; no añade una entidad SolicitudEmparejamiento. */
public class EmparejadorEnMemoria implements Emparejador {
    private final Set<UUID> esperando = new LinkedHashSet<>();
    private final SecureRandom azar = new SecureRandom();

    @Override
    public synchronized Optional<UUID> buscarOEsperar(UUID estudianteId) {
        Objects.requireNonNull(estudianteId);
        esperando.remove(estudianteId);
        if (!esperando.isEmpty()) {
            UUID elegido = null;
            int vistos = 0;
            for (UUID candidato : esperando) {
                vistos++;
                if (azar.nextInt(vistos) == 0) {
                    elegido = candidato;
                }
            }
            esperando.remove(elegido);
            return Optional.of(elegido);
        }
        esperando.add(estudianteId);
        return Optional.empty();
    }

    @Override public synchronized void cancelarEspera(UUID estudianteId) { esperando.remove(estudianteId); }
}
