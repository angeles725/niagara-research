package javax.baja.nre.function;

@FunctionalInterface
public interface FunctionCanThrowException<T, R, E extends Exception> {
   R apply(T var1) throws E;
}
