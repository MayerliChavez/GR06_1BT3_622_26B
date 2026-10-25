package epn.edu.ec.tdd.chat;

import java.util.UUID;

// Contrato de datos del chat; no implementa el procesamiento del mensaje.
public record Mensaje(String texto, UUID autorId, UUID sesionId) {
}
