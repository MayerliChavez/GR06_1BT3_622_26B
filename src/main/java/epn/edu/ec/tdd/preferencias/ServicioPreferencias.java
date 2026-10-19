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

    public SesionEstudio iniciarSesion(UUID estudianteId, Instant ahora) {
        var configuracion = preferencias.buscarPorEstudiante(estudianteId);
        return pomodoro.iniciarSesion(estudianteId, configuracion, ahora);
    }
}
