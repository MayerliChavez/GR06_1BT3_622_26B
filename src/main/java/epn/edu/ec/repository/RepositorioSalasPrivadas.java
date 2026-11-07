package epn.edu.ec.repository;

import epn.edu.ec.model.SalaPrivada;
import java.util.Optional;
import java.util.UUID;

public interface RepositorioSalasPrivadas {
    Optional<SalaPrivada> buscarAbiertaPorEstudiante(UUID estudianteId);
    Optional<SalaPrivada> buscarAbiertaPorCodigo(String codigo);
    void guardarNueva(SalaPrivada sala);
}
