package javax.baja.nre.function;

public interface SupplierCanThrowException<T, E extends Exception> {
   T get() throws E;
}
