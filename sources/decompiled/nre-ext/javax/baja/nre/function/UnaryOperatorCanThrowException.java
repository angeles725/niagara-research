package javax.baja.nre.function;

public interface UnaryOperatorCanThrowException<T, E extends Throwable> {
   T apply(T var1) throws E;

   static <T> UnaryOperatorCanThrowException<T, RuntimeException> identity() {
      return o -> o;
   }
}
