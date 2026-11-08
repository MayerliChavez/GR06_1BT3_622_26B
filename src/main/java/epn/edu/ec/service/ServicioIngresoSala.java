package epn.edu.ec.service;

import epn.edu.ec.model.SolicitudIngresoSala;
import epn.edu.ec.repository.*;
import java.time.Instant;
import java.util.*;

public class ServicioIngresoSala {

    private final RepositorioSolicitudesSalaJpa solicitudes;
    private final RepositorioSalasPrivadas salas;
    private final RepositorioSesiones sesiones;
    private final RepositorioSesionesCompartidas compartidas;

    public ServicioIngresoSala(
            RepositorioSolicitudesSalaJpa solicitudes,
            RepositorioSalasPrivadas salas,
            RepositorioSesiones sesiones,
            RepositorioSesionesCompartidas compartidas) {

        this.solicitudes = Objects.requireNonNull(solicitudes);
        this.salas = Objects.requireNonNull(salas);
        this.sesiones = Objects.requireNonNull(sesiones);
        this.compartidas = Objects.requireNonNull(compartidas);
    }

    public SolicitudIngresoSala solicitar(
            UUID estudianteId,
            String codigo,
            Instant ahora) {

        Objects.requireNonNull(estudianteId);
        Objects.requireNonNull(ahora);

        String normalizado = Objects.toString(codigo, "")
                .strip()
                .toUpperCase(Locale.ROOT);

        if (!normalizado.matches("[A-Z0-9]{6}")) {
            throw new IllegalArgumentException(
                    "El código ingresado no corresponde a ninguna sala abierta."
            );
        }

        synchronized (sesiones) {
            var pendiente =
                    solicitudes.buscarPendientePorEstudiante(estudianteId);

            if (pendiente.isPresent()) {
                if (pendiente.get().getSala().getCodigo()
                        .equals(normalizado)) {
                    return pendiente.get();
                }

                throw new IllegalStateException(
                        "Cancela tu solicitud pendiente antes de solicitar "
                                + "el ingreso a otra sala."
                );
            }

            boolean tieneActividad =
                    salas.buscarAbiertaPorEstudiante(estudianteId).isPresent()
                            || sesiones.buscarActivaPorEstudiante(estudianteId)
                            .isPresent()
                            || compartidas.buscarAbiertaPorEstudiante(estudianteId)
                            .isPresent();

            if (tieneActividad) {
                throw new IllegalStateException(
                        "Finaliza tu actividad actual antes de solicitar ingreso."
                );
            }

            var sala = salas.buscarAbiertaPorCodigo(normalizado)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "El código ingresado no corresponde "
                                    + "a ninguna sala abierta."
                    ));

            if (solicitudes.contarParticipantes(sala.getId()) >= 4) {
                throw new IllegalStateException(
                        "La sala está llena. No es posible enviar la solicitud."
                );
            }

            var solicitud =
                    new SolicitudIngresoSala(estudianteId, sala, ahora);

            solicitudes.guardar(solicitud);

            return solicitud;
        }
    }

    public Optional<SolicitudIngresoSala> consultarPendiente(
            UUID estudianteId) {

        return solicitudes.buscarPendientePorEstudiante(
                Objects.requireNonNull(estudianteId)
        );
    }

    public void cancelar(UUID estudianteId) {
        synchronized (sesiones) {
            solicitudes.cancelarPendiente(Objects.requireNonNull(estudianteId));
        }
    }

    public void aceptar(UUID responsableId, UUID solicitudId) {
        synchronized (sesiones) {
            solicitudes.decidir(Objects.requireNonNull(responsableId), Objects.requireNonNull(solicitudId), true);
        }
    }

    public void rechazar(UUID responsableId, UUID solicitudId) {
        synchronized (sesiones) {
            solicitudes.decidir(Objects.requireNonNull(responsableId), Objects.requireNonNull(solicitudId), false);
        }
    }

    public Optional<SolicitudIngresoSala> consultarUltima(UUID estudianteId) {
        return solicitudes.buscarUltimaPorEstudiante(Objects.requireNonNull(estudianteId));
    }

    public List<SolicitudIngresoSala> pendientesParaResponsable(
            UUID responsableId) {

        var sala = salas.buscarAbiertaPorEstudiante(
                Objects.requireNonNull(responsableId)
        ).orElseThrow(() -> new IllegalStateException(
                "No eres responsable de una sala abierta."
        ));

        if (!sala.getResponsableId().equals(responsableId)) {
            throw new SecurityException(
                    "Solo el responsable puede consultar "
                            + "las solicitudes de la sala."
            );
        }

        return solicitudes.pendientesDeSala(sala.getId());
    }
}
