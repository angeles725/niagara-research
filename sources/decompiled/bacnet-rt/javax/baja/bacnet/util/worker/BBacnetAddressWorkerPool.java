package javax.baja.bacnet.util.worker;

import java.util.ArrayList;
import java.util.List;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BComponent;
import javax.baja.sys.BIcon;
import javax.baja.sys.Context;
import javax.baja.sys.NotRunningException;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.util.CoalesceQueue;
import javax.baja.util.IFuture;
import javax.baja.util.Queue;
import javax.baja.util.ThreadPoolWorker;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "addressPools",
      type = "int",
      defaultValue = "DEFAULT_POOLS"
   ), @NiagaraProperty(
      name = "workersPerAddressPool",
      type = "int",
      defaultValue = "DEFAULT_WORKERS_PER_POOL"
   )})
public class BBacnetAddressWorkerPool extends BComponent implements IWorkerPool {
   private static final int DEFAULT_POOLS = 4;
   private static final int DEFAULT_WORKERS_PER_POOL = 2;
   public static final Property addressPools = newProperty(0, 4, null);
   public static final Property workersPerAddressPool = newProperty(0, 2, null);
   public static final Type TYPE = Sys.loadType(BBacnetAddressWorkerPool.class);
   private static final BIcon icon = BIcon.std("gears.png");
   private List<BBacnetAddressWorkerPool.AddressWorker> addressWorkers;
   private Object lock = new Object();

   public int getAddressPools() {
      return this.getInt(addressPools);
   }

   public void setAddressPools(int v) {
      this.setInt(addressPools, v, null);
   }

   public int getWorkersPerAddressPool() {
      return this.getInt(workersPerAddressPool);
   }

   public void setWorkersPerAddressPool(int v) {
      this.setInt(workersPerAddressPool, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public boolean isParentLegal(BComponent parent) {
      return BBacnetWorkerPool.isLegalParent(parent);
   }

   public void started() {
      this.startPools();
   }

   public void stopped() {
      this.stopPools();
   }

   public void changed(Property p, Context cx) {
      if (this.isRunning()) {
         if (p.equals(addressPools) || p.equals(workersPerAddressPool)) {
            this.restartPools();
         }
      }
   }

   private void restartPools() {
      synchronized (this.lock) {
         this.stopPools();
         this.startPools();
      }
   }

   private void startPools() {
      synchronized (this.lock) {
         int numberOfPools = this.getAddressPools();
         this.addressWorkers = new ArrayList<>(numberOfPools);
         IWorkerPoolAware parent = (IWorkerPoolAware)this.getParent();
         int maxQueueSize = parent.getQueue().maxSize();
         String threadName = parent.getWorkerThreadName();

         for (int i = 0; i < numberOfPools; i++) {
            Queue queue = new CoalesceQueue(maxQueueSize);
            ThreadPoolWorker worker = new ThreadPoolWorker(queue);
            worker.setMaxThreads(this.getWorkersPerAddressPool());
            worker.start(threadName + i);
            this.addressWorkers.add(new BBacnetAddressWorkerPool.AddressWorker(worker, queue));
         }

         parent.stopWorker();
      }
   }

   private void stopPools() {
      synchronized (this.lock) {
         if (this.addressWorkers != null) {
            int numberOfPools = this.addressWorkers.size();

            for (int i = 0; i < numberOfPools; i++) {
               BBacnetAddressWorkerPool.AddressWorker aw = this.addressWorkers.get(i);
               aw.stop();
            }

            this.addressWorkers.clear();
         }
      }
   }

   @Override
   public IFuture post(Runnable r) {
      if (!this.isRunning()) {
         throw new NotRunningException();
      } else {
         synchronized (this.lock) {
            if (r instanceof IBacnetAddress) {
               IBacnetAddress request = (IBacnetAddress)r;
               int workerIdx = request.getAddress().hash() % this.addressWorkers.size();
               BBacnetAddressWorkerPool.AddressWorker aw = this.addressWorkers.get(workerIdx);
               aw.enqueue(r);
            } else if (this.addressWorkers.size() > 0) {
               BBacnetAddressWorkerPool.AddressWorker defaultWorker = this.addressWorkers.get(0);
               defaultWorker.enqueue(r);
            }

            return null;
         }
      }
   }

   public BIcon getIcon() {
      return icon;
   }

   private static class AddressWorker {
      private ThreadPoolWorker worker;
      private Queue queue;

      public AddressWorker(ThreadPoolWorker worker, Queue queue) {
         this.worker = worker;
         this.queue = queue;
      }

      public void stop() {
         if (this.worker != null) {
            this.worker.stop();
         }
      }

      public void enqueue(Runnable r) {
         this.queue.enqueue(r);
      }
   }
}
