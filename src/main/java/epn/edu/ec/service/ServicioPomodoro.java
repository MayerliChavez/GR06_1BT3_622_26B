package epn.edu.ec.service;

import epn.edu.ec.model.*;
import epn.edu.ec.repository.RepositorioSesiones;
import java.time.*;
import java.util.*;

public class ServicioPomodoro {
    private final RepositorioSesiones repositorioSesiones;
    private final epn.edu.ec.repository.RepositorioSalasPrivadas salasPrivadas;
    public ServicioPomodoro(RepositorioSesiones repositorioSesiones) {
        this(repositorioSesiones, null);
    }
    public ServicioPomodoro(RepositorioSesiones repositorioSesiones,
            epn.edu.ec.repository.RepositorioSalasPrivadas salasPrivadas) {
        this.repositorioSesiones = Objects.requireNonNull(repositorioSesiones);
        this.salasPrivadas = salasPrivadas;
    }

    // Serializa las operaciones en esta aplicación local y evita dobles inicios entre pestañas.
    public synchronized SesionEstudio iniciarSesion(UUID estudianteId, ConfiguracionPomodoro configuracion, Instant ahora) {
        synchronized (repositorioSesiones) {
            if (salasPrivadas != null && salasPrivadas.buscarAbiertaPorEstudiante(estudianteId).isPresent())
                throw new IllegalStateException("Ya participas en una sala privada. Vuelve a tu sala actual.");
            if (repositorioSesiones.buscarActivaPorEstudiante(estudianteId).isPresent())
                throw new IllegalStateException("Ya tienes una sesión activa.");
            var historial = repositorioSesiones.buscarPorEstudiante(estudianteId);
            var estudiante = historial.isEmpty() ? new Estudiante(estudianteId, "Estudiante") : historial.get(0).getEstudiante();
            var sesion = new SesionEstudio(estudiante, configuracion, ahora);
            sesion.iniciarSiguienteBloque(ahora);
            repositorioSesiones.guardar(sesion);
            return sesion;
        }
    }

    public synchronized void pausarTemporizador(UUID sesionId, Instant ahora) {
        var sesion = obtenerSesionOExigirExistencia(sesionId);
        sesion.pausar(ahora);
        repositorioSesiones.guardar(sesion);
    }

    public synchronized void reanudarTemporizador(UUID sesionId, Instant ahora) {
        var sesion = obtenerSesionOExigirExistencia(sesionId);
        sesion.reanudar(ahora);
        repositorioSesiones.guardar(sesion);
    }

    public synchronized void completarBloqueActual(UUID sesionId, Instant ahora) {
        var sesion = obtenerSesionOExigirExistencia(sesionId);
        sesion.completarBloqueActual(ahora);
        repositorioSesiones.guardar(sesion);
    }

    // El orden esperado evita duplicar una transicion solicitada desde varias pestanas.
    public synchronized void avanzarSiCorresponde(UUID sesionId, int ordenEsperado, Instant ahora) {
        var sesion = obtenerSesionOExigirExistencia(sesionId);
        if (sesion.getEstado() != EstadoSesion.ACTIVA || sesion.getBloques().isEmpty()) return;
        var actual = sesion.getBloques().get(sesion.getBloques().size() - 1);
        if (actual.getOrden() != ordenEsperado) return;
        if (actual.getEstado() == EstadoBloque.EN_EJECUCION && actual.tiempoRestante(ahora).isZero()) {
            sesion.completarBloqueActual(ahora);
        } else if (actual.getEstado() != EstadoBloque.COMPLETADO) {
            return;
        }
        sesion.iniciarSiguienteBloque(ahora);
        repositorioSesiones.guardar(sesion);
    }

    public synchronized BloquePomodoro iniciarSiguienteBloque(UUID sesionId, Instant ahora) {
        var sesion = obtenerSesionOExigirExistencia(sesionId);
        var bloque = sesion.iniciarSiguienteBloque(ahora);
        repositorioSesiones.guardar(sesion);
        return bloque;
    }

    public synchronized Optional<Duration> consultarTiempoRestante(
            UUID sesionId,
            Instant ahora
    ) {
        var sesion = repositorioSesiones.buscarPorId(sesionId)
                .orElseThrow(
                        () -> new IllegalArgumentException(
                                "Sesión inexistente."
                        )
                );

        return sesion.tiempoRestanteBloqueActual(ahora);
    }

    public synchronized void finalizarSesion(UUID sesionId, Instant ahora) {
        var sesion = obtenerSesionOExigirExistencia(sesionId);
        sesion.finalizar(ahora);
        repositorioSesiones.guardar(sesion);
    }

    public synchronized void abandonarSesion(UUID sesionId, Instant ahora) {
        var sesion = obtenerSesionOExigirExistencia(sesionId);
        sesion.abandonar(ahora);
        repositorioSesiones.guardar(sesion);
    }

    public synchronized List<SesionEstudio> consultarHistorial(UUID estudianteId) {
        return repositorioSesiones.buscarPorEstudiante(estudianteId);
    }

    private SesionEstudio obtenerSesionOExigirExistencia(UUID sesionId) {
        var sesion = repositorioSesiones.buscarPorId(sesionId)
                .orElseThrow(
                        () -> new IllegalArgumentException(
                                "Sesión inexistente."
                        )
                );

        if (salasPrivadas != null
                && salasPrivadas.buscarAbiertaPorEstudiante(
                sesion.getEstudiante().getId()
        ).isPresent()) {
            throw new IllegalStateException(
                    "Usa la sala privada para gestionar el ciclo común."
            );
        }

        return sesion;
    }
}
