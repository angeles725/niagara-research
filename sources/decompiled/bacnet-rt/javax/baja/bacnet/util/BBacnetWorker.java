package javax.baja.bacnet.util;

import javax.baja.bacnet.util.worker.IWorkerPool;
import javax.baja.bacnet.util.worker.IWorkerPoolAware;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BObject;
import javax.baja.sys.BValue;
import javax.baja.sys.Context;
import javax.baja.sys.NotRunningException;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.util.BWorker;
import javax.baja.util.CoalesceQueue;
import javax.baja.util.IFuture;
import javax.baja.util.Queue;
import javax.baja.util.Worker;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "maxQueueSize",
      type = "int",
      defaultValue = "1000"
   ), @NiagaraProperty(
      name = "workerThreadName",
      type = "String",
      defaultValue = "",
      flags = 3
   )})
public class BBacnetWorker extends BWorker implements IWorkerPoolAware {
   public static final Property maxQueueSize = newProperty(0, 1000, null);
   public static final Property workerThreadName = newProperty(3, "", null);
   public static final Type TYPE = Sys.loadType(BBacnetWorker.class);
   private Object lock = new Object();
   private IWorkerPool workerPool = null;
   protected CoalesceQueue queue;
   protected Worker worker;

   public int getMaxQueueSize() {
      return this.getInt(maxQueueSize);
   }

   public void setMaxQueueSize(int v) {
      this.setInt(maxQueueSize, v, null);
   }

   @Override
   public String getWorkerThreadName() {
      return this.getString(workerThreadName);
   }

   public void setWorkerThreadName(String v) {
      this.setString(workerThreadName, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BBacnetWorker() {
   }

   public BBacnetWorker(int queueSize) {
      this.setMaxQueueSize(queueSize);
   }

   public BBacnetWorker(String workerName) {
      this.setWorkerThreadName(workerName);
   }

   public void started() throws Exception {
      if (this.getWorkerThreadName().length() == 0) {
         this.setWorkerThreadName(this.getParent().getName() + ":" + this.getName());
      }

      super.started();
   }

   public void changed(Property p, Context cx) {
      super.changed(p, cx);
      if (p == maxQueueSize) {
         if (!this.isRunning()) {
            return;
         }

         synchronized (this.lock) {
            if (this.workerPool == null) {
               this.stopWorker();
               this.queue = null;
               this.worker = null;
               this.getWorker();
               this.startWorker();
            }
         }
      }
   }

   public void added(Property property, Context context) {
      super.added(property, context);
      BObject o = null;
      BValue var8;
      if ((var8 = this.get(property)) instanceof IWorkerPool) {
         IWorkerPool pool = (IWorkerPool)var8;
         synchronized (this.lock) {
            this.workerPool = pool;
            if (this.worker != null && this.worker.isRunning()) {
               this.worker.stop();
            }
         }
      }
   }

   public void removed(Property property, BValue oldValue, Context context) {
      super.removed(property, oldValue, context);
      if (oldValue instanceof IWorkerPool) {
         synchronized (this.lock) {
            this.workerPool = null;
            this.startWorker();
         }
      }
   }

   @Override
   public void stopWorker() {
      synchronized (this.lock) {
         if (this.worker != null && this.worker.isRunning()) {
            this.worker.stop();
         }
      }
   }

   public IFuture post(Runnable r) {
      if (!this.isRunning()) {
         throw new NotRunningException();
      } else if (this.queue == null) {
         throw new NotRunningException();
      } else {
         synchronized (this.lock) {
            if (this.workerPool != null && this.workerPool.isRunning()) {
               this.workerPool.post(r);
            } else {
               this.queue.enqueue(r);
            }

            return null;
         }
      }
   }

   public Worker getWorker() {
      if (this.worker == null) {
         this.queue = new CoalesceQueue(this.getMaxQueueSize());
         this.worker = new Worker(this.queue);
      }

      return this.worker;
   }

   public void setWorker(Worker worker) {
      this.worker = worker;
   }

   public void dump() {
      synchronized (this.queue) {
         Object[] a = this.queue.toArray();
         System.out.println("BBacnetWorker dump (" + a.length + " entries):");

         for (int i = 0; i < a.length; i++) {
            System.out.println("" + i + ": " + a[i]);
         }
      }
   }

   public int getQueueSize() {
      return this.queue.size();
   }

   @Override
   public Queue getQueue() {
      return this.queue;
   }

   @Override
   public boolean hasWorkerPool() {
      synchronized (this.lock) {
         return this.workerPool != null;
      }
   }
}
