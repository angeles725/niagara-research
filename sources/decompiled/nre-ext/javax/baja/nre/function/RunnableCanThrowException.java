package javax.baja.nre.function;

public interface RunnableCanThrowException<E extends Exception> {
   void run() throws E;
}
