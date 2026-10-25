package epn.edu.ec.tdd.chat;

public class ValidadorMensajes {

    public void validar(String texto) {
        if (texto == null || texto.codePoints().allMatch(c -> Character.isWhitespace(c) || Character.isSpaceChar(c))) {
            throw new IllegalArgumentException("El mensaje no puede estar vacío ni contener solo espacios.");
        }
    }
}
