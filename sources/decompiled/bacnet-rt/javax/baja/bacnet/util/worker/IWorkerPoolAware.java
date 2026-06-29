package javax.baja.bacnet.util.worker;

import javax.baja.util.Queue;

public interface IWorkerPoolAware {
   String getWorkerThreadName();

   void stopWorker();

   Queue getQueue();

   boolean hasWorkerPool();
}
