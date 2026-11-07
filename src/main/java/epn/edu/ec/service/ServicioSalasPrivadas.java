package epn.edu.ec.service;

import epn.edu.ec.model.SalaPrivada;
import epn.edu.ec.repository.*;
import java.util.Optional;
import java.util.UUID;

public class ServicioSalasPrivadas {
    private final RepositorioSalasPrivadas salas;
    private final RepositorioSesiones sesiones;
    private final RepositorioSesionesCompartidas compartidas;
    private final GeneradorCodigoSala codigos;

    public ServicioSalasPrivadas(RepositorioSalasPrivadas salas, RepositorioSesiones sesiones,
            RepositorioSesionesCompartidas compartidas, GeneradorCodigoSala codigos) {
        this.salas = java.util.Objects.requireNonNull(salas);
        this.sesiones = java.util.Objects.requireNonNull(sesiones);
        this.compartidas = java.util.Objects.requireNonNull(compartidas);
        this.codigos = java.util.Objects.requireNonNull(codigos);
    }

    public SalaPrivada crear(UUID estudianteId) {
        java.util.Objects.requireNonNull(estudianteId);
        // Los tres servicios comparten este coordinador en la instancia web.
        synchronized (sesiones) {
            if (salas.buscarAbiertaPorEstudiante(estudianteId).isPresent())
                throw new IllegalStateException("Ya participas en una sala privada. Vuelve a tu sala actual.");
            if (sesiones.buscarActivaPorEstudiante(estudianteId).isPresent()
                    || compartidas.buscarAbiertaPorEstudiante(estudianteId).isPresent())
                throw new IllegalStateException("Ya tienes una sesión de estudio en curso. Finalízala antes de crear una sala.");
            for (int intento = 0; intento < 20; intento++) {
                var sala = new SalaPrivada(estudianteId, codigos.generar());
                if (salas.buscarAbiertaPorCodigo(sala.getCodigo()).isPresent()) continue;
                try {
                    salas.guardarNueva(sala);
                    return sala;
                } catch (CodigoSalaOcupadoException colisionConcurrente) {
                    // La restricción de la base también protege una colisión entre transacciones.
                }
            }
            throw new IllegalStateException("No se pudo generar un código disponible. Inténtalo nuevamente.");
        }
    }

    public Optional<SalaPrivada> consultarActual(UUID estudianteId) {
        return salas.buscarAbiertaPorEstudiante(java.util.Objects.requireNonNull(estudianteId));
    }
}
