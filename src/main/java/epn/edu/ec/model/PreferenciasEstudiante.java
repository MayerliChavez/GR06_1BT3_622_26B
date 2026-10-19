package epn.edu.ec.model;
import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "preferencias_estudiante")
public class PreferenciasEstudiante {
    @Id private UUID estudianteId;
    @Embedded private ConfiguracionPomodoro configuracion;
    protected PreferenciasEstudiante() {}
    public PreferenciasEstudiante(UUID estudianteId, ConfiguracionPomodoro configuracion) {
        this.estudianteId = java.util.Objects.requireNonNull(estudianteId);
        this.configuracion = java.util.Objects.requireNonNull(configuracion).copiar();
    }
    public ConfiguracionPomodoro getConfiguracion() { return configuracion.copiar(); }
}
