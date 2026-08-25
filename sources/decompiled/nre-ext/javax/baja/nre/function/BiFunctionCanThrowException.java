package javax.baja.nre.function;

@FunctionalInterface
public interface BiFunctionCanThrowException<T, U, R, E extends Exception> {
   R apply(T var1, U var2) throws E;
}
