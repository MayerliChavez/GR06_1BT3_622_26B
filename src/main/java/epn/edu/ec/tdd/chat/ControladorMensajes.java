package epn.edu.ec.tdd.chat;

import java.util.UUID;

public class ControladorMensajes {

    public Mensaje crearMensaje(String texto, UUID autorId, UUID sesionId) {
        return new Mensaje(texto, autorId, sesionId);
    }
}
