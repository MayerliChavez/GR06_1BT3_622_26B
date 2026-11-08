package epn.edu.ec.web;

import epn.edu.ec.service.ServicioCicloSalaPrivada.EstadoCiclo;
import java.time.Instant;
import java.util.Objects;

public class SerializadorCicloSala {
    public String serializar(EstadoCiclo estado) {
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
                + instante(estado.horaServidor())
                + "}";
    }

    private String instante(Instant valor) {
        return valor == null
                ? "null"
                : Long.toString(valor.toEpochMilli());
    }
}