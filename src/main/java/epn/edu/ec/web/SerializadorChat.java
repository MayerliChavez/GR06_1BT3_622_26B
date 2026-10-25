package epn.edu.ec.web;

import epn.edu.ec.model.MensajeChat;

public class SerializadorChat {

    public String mensaje(MensajeChat m) {
        return "{\"id\":" + m.getId()
                + ",\"texto\":" + cadena(m.getTexto())
                + ",\"autorId\":" + cadena(m.getAutorId().toString())
                + ",\"autor\":" + cadena(m.getNombreAutor())
                + ",\"fecha\":" + cadena(m.getFecha().toString())
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
                        resultado.append(String.format("\\u%04x", (int) c));
                    } else {
                        resultado.append(c);
                    }
                }
            }
        }

        return resultado.append('"').toString();
    }
}
