package epn.edu.ec.tdd.preferencias;

import epn.edu.ec.model.SesionEstudio;
import epn.edu.ec.service.ServicioPomodoro;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class ServicioPreferencias {
    private final RepositorioPreferencias preferencias;
    private final ServicioPomodoro pomodoro;

    public ServicioPreferencias(RepositorioPreferencias preferencias, ServicioPomodoro pomodoro) {
        this.preferencias = Objects.requireNonNull(preferencias);
        this.pomodoro = Objects.requireNonNull(pomodoro);
    }

    public epn.edu.ec.model.ConfiguracionPomodoro consultar(UUID estudianteId) {
        return preferencias.buscarPorEstudiante(estudianteId);
    }

    public void guardar(UUID estudianteId, int concentracion, int corto, int largo, int bloques) {
        var configuracion = new epn.edu.ec.tdd.timer.Timer().configurar(concentracion, corto, largo, bloques);
        preferencias.guardar(estudianteId, configuracion);
    }

    public SesionEstudio iniciarSesion(UUID estudianteId, Instant ahora) {
        var configuracion = preferencias.buscarPorEstudiante(estudianteId);
        return pomodoro.iniciarSesion(estudianteId, configuracion, ahora);
    }
}
