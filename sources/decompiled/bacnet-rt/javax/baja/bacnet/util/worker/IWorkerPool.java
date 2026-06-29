package javax.baja.bacnet.util.worker;

import javax.baja.util.IFuture;

public interface IWorkerPool {
   IFuture post(Runnable var1);

   boolean isRunning();
}
