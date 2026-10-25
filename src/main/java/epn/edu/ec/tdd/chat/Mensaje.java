package epn.edu.ec.tdd.chat;

import java.util.UUID;

public record Mensaje(String texto, UUID autorId, UUID sesionId) {
}
