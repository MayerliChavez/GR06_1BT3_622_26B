package edu.proyecto.service;

import edu.proyecto.model.*;
import edu.proyecto.repository.RepositorioSesiones;
import java.time.*;
import java.util.*;

public class ServicioPomodoro {
    private final RepositorioSesiones repositorioSesiones;
    public ServicioPomodoro(RepositorioSesiones repositorioSesiones) {
        this.repositorioSesiones = Objects.requireNonNull(repositorioSesiones);
    }

    // Serializa las operaciones en esta aplicación local y evita dobles inicios entre pestañas.
    public synchronized SesionEstudio iniciarSesion(UUID estudianteId, ConfiguracionPomodoro configuracion, Instant ahora) {
        if (repositorioSesiones.buscarActivaPorEstudiante(estudianteId).isPresent())
            throw new IllegalStateException("Ya tienes una sesión activa.");
        var historial = repositorioSesiones.buscarPorEstudiante(estudianteId);
        var estudiante = historial.isEmpty() ? new Estudiante(estudianteId, "Estudiante") : historial.get(0).getEstudiante();
        var sesion = new SesionEstudio(estudiante, configuracion, ahora);
        sesion.iniciarSiguienteBloque(ahora);
        repositorioSesiones.guardar(sesion);
        return sesion;
    }

    public synchronized void pausarTemporizador(UUID sesionId, Instant ahora) {
        var sesion = repositorioSesiones.buscarPorId(sesionId).orElseThrow(() -> new IllegalArgumentException("Sesión inexistente."));
        sesion.pausar(ahora);
        repositorioSesiones.guardar(sesion);
    }

    public synchronized void reanudarTemporizador(UUID sesionId, Instant ahora) {
        var sesion = repositorioSesiones.buscarPorId(sesionId).orElseThrow(() -> new IllegalArgumentException("Sesión inexistente."));
        sesion.reanudar(ahora);
        repositorioSesiones.guardar(sesion);
    }

    public synchronized void completarBloqueActual(UUID sesionId, Instant ahora) {
        var sesion = repositorioSesiones.buscarPorId(sesionId).orElseThrow(() -> new IllegalArgumentException("Sesión inexistente."));
        sesion.completarBloqueActual(ahora);
        repositorioSesiones.guardar(sesion);
    }

    public synchronized BloquePomodoro iniciarSiguienteBloque(UUID sesionId, Instant ahora) {
        var sesion = repositorioSesiones.buscarPorId(sesionId).orElseThrow(() -> new IllegalArgumentException("Sesión inexistente."));
        var bloque = sesion.iniciarSiguienteBloque(ahora);
        repositorioSesiones.guardar(sesion);
        return bloque;
    }

    public synchronized Optional<Duration> consultarTiempoRestante(UUID sesionId, Instant ahora) {
        var sesion = repositorioSesiones.buscarPorId(sesionId).orElseThrow(() -> new IllegalArgumentException("Sesión inexistente."));
        return sesion.tiempoRestanteBloqueActual(ahora);
    }

    public synchronized void finalizarSesion(UUID sesionId, Instant ahora) {
        var sesion = repositorioSesiones.buscarPorId(sesionId).orElseThrow(() -> new IllegalArgumentException("Sesión inexistente."));
        sesion.finalizar(ahora);
        repositorioSesiones.guardar(sesion);
    }

    public synchronized void abandonarSesion(UUID sesionId, Instant ahora) {
        var sesion = repositorioSesiones.buscarPorId(sesionId).orElseThrow(() -> new IllegalArgumentException("Sesión inexistente."));
        sesion.abandonar(ahora);
        repositorioSesiones.guardar(sesion);
    }

    public synchronized List<SesionEstudio> consultarHistorial(UUID estudianteId) {
        return repositorioSesiones.buscarPorEstudiante(estudianteId);
    }
}
