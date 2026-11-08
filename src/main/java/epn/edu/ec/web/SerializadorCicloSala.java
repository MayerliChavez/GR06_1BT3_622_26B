package epn.edu.ec.web;

import epn.edu.ec.model.MensajeSalaPrivada;
import epn.edu.ec.service.ServicioCicloSalaPrivada.EstadoCiclo;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class SerializadorCicloSala {
    public record EventoChat(
            String tipo,
            Long mensajeId,
            String mensaje
    ) {}

    public String serializar(EstadoCiclo estado) {
        return campos(estado) + "}";
    }

    public String serializar(
            EstadoCiclo estado,
            List<MensajeSalaPrivada> mensajes,
            List<EventoChat> eventos
    ) {
        var chat = new SerializadorChat();

        String historial = mensajes.stream()
                .map(chat::mensaje)
                .collect(Collectors.joining(","));

        String respuestas = eventos.stream()
                .map(evento ->
                        "{\"tipo\":" + chat.cadena(evento.tipo())
                                + ",\"mensajeId\":"
                                + evento.mensajeId()
                                + ",\"mensaje\":"
                                + (evento.mensaje() == null
                                ? "null"
                                : chat.cadena(evento.mensaje()))
                                + "}"
                )
                .collect(Collectors.joining(","));

        return campos(estado)
                + ",\"mensajes\":[" + historial
                + "],\"eventosChat\":[" + respuestas
                + "]}";
    }

    private String campos(EstadoCiclo estado) {
        Objects.requireNonNull(estado);

        String bloque = estado.tipo() == null
                ? "null"
                : "\"" + estado.tipo().name() + "\"";

        return "{\"tipo\":\"estadoCiclo\""
                + ",\"salaId\":\"" + estado.salaId() + "\""
                + ",\"iniciado\":" + estado.iniciado()
                + ",\"bloque\":" + bloque
                + ",\"inicioBloque\":"
                + instante(estado.inicioBloque())
                + ",\"finBloque\":"
                + instante(estado.finBloque())
                + ",\"restanteMillis\":" + estado.restanteMillis()
                + ",\"orden\":" + estado.orden()
                + ",\"horaServidor\":"
                + instante(estado.horaServidor());
    }

    private String instante(Instant valor) {
        return valor == null
                ? "null"
                : Long.toString(valor.toEpochMilli());
    }
}