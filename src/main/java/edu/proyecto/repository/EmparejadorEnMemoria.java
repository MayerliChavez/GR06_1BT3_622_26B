package edu.proyecto.repository;
import java.security.SecureRandom;
import java.util.*;

/** Adaptador de la cola de búsqueda; no añade una entidad SolicitudEmparejamiento. */
public class EmparejadorEnMemoria implements Emparejador {
    private final Set<UUID> esperando = new LinkedHashSet<>();
    private final SecureRandom azar = new SecureRandom();
    @Override public synchronized Optional<UUID> buscarOEsperar(UUID estudianteId) {
        Objects.requireNonNull(estudianteId);
        // Repetir la solicitud conserva una única posición y no empareja al estudiante consigo mismo.
        esperando.remove(estudianteId);
        if (!esperando.isEmpty()) {
            var candidatos = new ArrayList<>(esperando);
            UUID elegido = candidatos.get(azar.nextInt(candidatos.size()));
            esperando.remove(elegido);
            return Optional.of(elegido);
        }
        esperando.add(estudianteId);
        return Optional.empty();
    }
    @Override public synchronized void cancelarEspera(UUID estudianteId) { esperando.remove(estudianteId); }
}
