package epn.edu.ec.repository;

public class CodigoSalaOcupadoException extends IllegalStateException {
    public CodigoSalaOcupadoException() { super("El código generado ya está en uso."); }
}
