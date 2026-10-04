package edu.proyecto.repository;
import edu.proyecto.model.SesionCompartida;
import java.util.*;
public interface RepositorioSesionesCompartidas {
    void guardar(SesionCompartida sesion);
    Optional<SesionCompartida> buscarPorId(UUID id);
    Optional<SesionCompartida> buscarAbiertaPorEstudiante(UUID estudianteId);
}
