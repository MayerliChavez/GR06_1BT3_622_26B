package epn.edu.ec.tdd.preferencias;

import epn.edu.ec.model.ConfiguracionPomodoro;
import java.util.UUID;

public interface RepositorioPreferencias {
    ConfiguracionPomodoro buscarPorEstudiante(UUID estudianteId);
    void guardar(UUID estudianteId, ConfiguracionPomodoro configuracion);
}
