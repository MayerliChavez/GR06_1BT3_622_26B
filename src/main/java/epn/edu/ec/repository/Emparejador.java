package epn.edu.ec.repository;
import java.util.*;
public interface Emparejador {
    Optional<UUID> buscarOEsperar(UUID estudianteId);
    void cancelarEspera(UUID estudianteId);
}
