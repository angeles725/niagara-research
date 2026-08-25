package javax.baja.nre.function;

@FunctionalInterface
public interface ConsumerCanThrowException<T, E extends Exception> {
   void accept(T var1) throws E;
}
