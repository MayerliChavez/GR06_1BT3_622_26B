package edu.proyecto.repository;

import edu.proyecto.model.SesionEstudio;
import java.util.*;

public interface RepositorioSesiones {
    void guardar(SesionEstudio sesion);
    Optional<SesionEstudio> buscarPorId(UUID id);
    List<SesionEstudio> buscarPorEstudiante(UUID estudianteId);
    Optional<SesionEstudio> buscarActivaPorEstudiante(UUID estudianteId);
}
