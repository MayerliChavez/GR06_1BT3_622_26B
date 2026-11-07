package epn.edu.ec.tdd.chat;

public class ValidadorMensajes {

    public void validar(String texto) {
        if (texto == null || texto.codePoints().allMatch(c -> Character.isWhitespace(c) || Character.isSpaceChar(c))) {
            throw new IllegalArgumentException("Escribe un mensaje antes de enviarlo.");
        }
        if (texto.length() > 2000) {
            throw new IllegalArgumentException("El mensaje no puede superar los 2000 caracteres.");
        }
    }
}
