package javax.baja.nre.function;

@FunctionalInterface
public interface BiConsumerCanThrowException<T, U, E extends Exception> {
   void accept(T var1, U var2) throws E;
}
