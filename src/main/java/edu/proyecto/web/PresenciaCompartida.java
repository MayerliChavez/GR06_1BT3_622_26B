package edu.proyecto.web;
import edu.proyecto.service.ServicioEstudioCompartido;
import java.time.*;
import java.util.*;

/** Detector técnico de presencia: no es una entidad ni una operación nueva del modelo. */
public final class PresenciaCompartida {
    private final ServicioEstudioCompartido servicio;
    private final Map<UUID, Instant> ultimasSenales = new HashMap<>();
    private final Map<UUID, UUID> sesiones = new HashMap<>();
    private final Duration tolerancia;
    public PresenciaCompartida(ServicioEstudioCompartido servicio, Duration tolerancia) {
        this.servicio = Objects.requireNonNull(servicio);
        if (tolerancia.isNegative() || tolerancia.isZero()) throw new IllegalArgumentException("Tolerancia positiva requerida.");
        this.tolerancia = tolerancia;
    }
    public void registrar(UUID estudianteId, UUID compartidaId, Instant ahora) {
        synchronized (servicio) {
            ultimasSenales.put(estudianteId, ahora);
            if (compartidaId != null) sesiones.put(estudianteId, compartidaId);
            else sesiones.remove(estudianteId);
        }
    }
    public void olvidar(UUID estudianteId) {
        synchronized (servicio) { ultimasSenales.remove(estudianteId); sesiones.remove(estudianteId); }
    }
    public void revisar(Instant ahora) {
        synchronized (servicio) {
            for (var entrada : new ArrayList<>(ultimasSenales.entrySet())) {
                Instant vencimiento = entrada.getValue().plus(tolerancia);
                if (ahora.isBefore(vencimiento)) continue;
                UUID id = entrada.getKey(), compartidaId = sesiones.get(id);
                if (compartidaId == null) servicio.cancelarEspera(id);
                else servicio.registrarDesconexion(compartidaId, id, ahora);
                olvidar(id);
            }
        }
    }
}
