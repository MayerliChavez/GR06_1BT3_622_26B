package epn.edu.ec.web;

import epn.edu.ec.model.MensajeChat;
import epn.edu.ec.model.MensajeSalaPrivada;
import java.time.Instant;
import java.util.UUID;

public class SerializadorChat {

    public String mensaje(MensajeChat mensaje) {
        return mensaje(
                mensaje.getId(),
                mensaje.getTexto(),
                mensaje.getAutorId(),
                mensaje.getNombreAutor(),
                mensaje.getFecha()
        );
    }

    public String mensaje(MensajeSalaPrivada mensaje) {
        return mensaje(
                mensaje.getId(),
                mensaje.getTexto(),
                mensaje.getAutorId(),
                mensaje.getNombreAutor(),
                mensaje.getFecha()
        );
    }

    private String mensaje(
            Long id,
            String texto,
            UUID autorId,
            String autor,
            Instant fecha
    ) {
        return "{\"id\":" + id
                + ",\"texto\":" + cadena(texto)
                + ",\"autorId\":" + cadena(autorId.toString())
                + ",\"autor\":" + cadena(autor)
                + ",\"fecha\":" + cadena(fecha.toString())
                + "}";
    }

    public String cadena(String valor) {
        StringBuilder resultado = new StringBuilder("\"");

        for (char c : valor.toCharArray()) {
            switch (c) {
                case '"' -> resultado.append("\\\"");
                case '\\' -> resultado.append("\\\\");
                case '\n' -> resultado.append("\\n");
                case '\r' -> resultado.append("\\r");
                case '\t' -> resultado.append("\\t");
                default -> {
                    if (c < 32) {
                        resultado.append(
                                String.format("\\u%04x", (int) c)
                        );
                    } else {
                        resultado.append(c);
                    }
                }
            }
        }

        return resultado.append('"').toString();
    }
}