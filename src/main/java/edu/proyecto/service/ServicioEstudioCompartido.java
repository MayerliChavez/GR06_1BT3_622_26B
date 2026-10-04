package edu.proyecto.service;
import edu.proyecto.model.*;
import edu.proyecto.repository.*;
import java.time.*;
import java.util.*;

public class ServicioEstudioCompartido {
    private final Emparejador emparejador;
    private final RepositorioSesiones repositorioSesiones;
    private final RepositorioSesionesCompartidas repositorioCompartidas;
    private final ConfiguracionPomodoro configuracionFija;

    public ServicioEstudioCompartido(Emparejador emparejador, RepositorioSesiones repositorioSesiones,
            RepositorioSesionesCompartidas repositorioCompartidas, ConfiguracionPomodoro configuracionFija) {
        this.emparejador = Objects.requireNonNull(emparejador);
        this.repositorioSesiones = Objects.requireNonNull(repositorioSesiones);
        this.repositorioCompartidas = Objects.requireNonNull(repositorioCompartidas);
        this.configuracionFija = Objects.requireNonNull(configuracionFija);
    }
    public synchronized Optional<SesionCompartida> solicitarEmparejamiento(UUID estudianteId, Instant ahora) {
        var existente = repositorioCompartidas.buscarAbiertaPorEstudiante(estudianteId);
        if (existente.isPresent()) return existente;
        if (repositorioSesiones.buscarActivaPorEstudiante(estudianteId).isPresent()) {
            emparejador.cancelarEspera(estudianteId);
            throw new IllegalStateException("Finaliza tu sesión individual antes de buscar compañero.");
        }
        Optional<UUID> candidato;
        do {
            candidato = emparejador.buscarOEsperar(estudianteId);
            if (candidato.isEmpty()) return Optional.empty();
        } while (repositorioSesiones.buscarActivaPorEstudiante(candidato.get()).isPresent()
                || repositorioCompartidas.buscarAbiertaPorEstudiante(candidato.get()).isPresent());
        UUID companeroId = candidato.orElseThrow();
        var historialA = repositorioSesiones.buscarPorEstudiante(estudianteId);
        var historialB = repositorioSesiones.buscarPorEstudiante(companeroId);
        var a = historialA.isEmpty() ? new Estudiante(estudianteId, "Estudiante") : historialA.get(0).getEstudiante();
        var b = historialB.isEmpty() ? new Estudiante(companeroId, "Estudiante") : historialB.get(0).getEstudiante();
        var compartida = new SesionCompartida(new SesionEstudio(a, configuracionFija, ahora), new SesionEstudio(b, configuracionFija, ahora));
        compartida.iniciar(ahora);
        repositorioCompartidas.guardar(compartida); // Una única transacción para la sesión y ambas composiciones.
        return Optional.of(compartida);
    }
    public synchronized void cancelarEspera(UUID estudianteId) { emparejador.cancelarEspera(estudianteId); }
    public synchronized void actualizarSesion(UUID compartidaId, Instant ahora) {
        var sesion = repositorioCompartidas.buscarPorId(compartidaId).orElseThrow(() -> new IllegalArgumentException("Sesión compartida inexistente."));
        sesion.avanzarSiCorresponde(ahora);
        repositorioCompartidas.guardar(sesion);
    }
    public synchronized void salir(UUID compartidaId, UUID estudianteId, Instant ahora) {
        var sesion = repositorioCompartidas.buscarPorId(compartidaId).orElseThrow(() -> new IllegalArgumentException("Sesión compartida inexistente."));
        sesion.registrarSalida(estudianteId, MotivoSalida.VOLUNTARIA, ahora);
        repositorioCompartidas.guardar(sesion);
    }
    public synchronized void registrarDesconexion(UUID compartidaId, UUID estudianteId, Instant ahora) {
        var sesion = repositorioCompartidas.buscarPorId(compartidaId).orElseThrow(() -> new IllegalArgumentException("Sesión compartida inexistente."));
        sesion.registrarSalida(estudianteId, MotivoSalida.DESCONEXION, ahora);
        repositorioCompartidas.guardar(sesion);
    }
    public synchronized Optional<Duration> consultarTiempoRestante(UUID compartidaId, Instant ahora) {
        var sesion = repositorioCompartidas.buscarPorId(compartidaId).orElseThrow(() -> new IllegalArgumentException("Sesión compartida inexistente."));
        return sesion.tiempoRestanteBloqueActual(ahora);
    }
}
