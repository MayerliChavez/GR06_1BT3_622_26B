package epn.edu.ec.repository;

import epn.edu.ec.model.SesionEstudio;
import java.util.*;

public interface RepositorioSesiones {
    void guardar(SesionEstudio sesion);
    Optional<SesionEstudio> buscarPorId(UUID id);
    List<SesionEstudio> buscarPorEstudiante(UUID estudianteId);
    Optional<SesionEstudio> buscarActivaPorEstudiante(UUID estudianteId);
}
