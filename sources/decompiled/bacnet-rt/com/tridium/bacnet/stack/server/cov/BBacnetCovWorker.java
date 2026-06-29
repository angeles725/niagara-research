package com.tridium.bacnet.stack.server.cov;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.bacnet.datatypes.BBacnetAddress;
import javax.baja.bacnet.export.Cov;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.util.IntHashMap;
import javax.baja.nre.util.IntHashMap.Iterator;
import javax.baja.sys.Action;
import javax.baja.sys.BComponent;
import javax.baja.sys.BRelTime;
import javax.baja.sys.Clock;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.sys.Clock.Ticket;
import javax.baja.util.ICoalesceable;
import javax.baja.util.Queue;
import javax.baja.util.QueueFullException;

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
   ), @NiagaraProperty(
      name = "numberOfThreads",
      type = "int",
      defaultValue = "2"
   )})
@NiagaraAction(
   name = "removeStaleQueues"
)
public class BBacnetCovWorker extends BComponent {
   public static final Property maxQueueSize = newProperty(0, 1000, null);
   public static final Property workerThreadName = newProperty(3, "", null);
   public static final Property numberOfThreads = newProperty(0, 2, null);
   public static final Action removeStaleQueues = newAction(0, null);
   public static final Type TYPE = Sys.loadType(BBacnetCovWorker.class);
   private Ticket cleanupTimer = null;
   private static final int DEVICE_STALE_TIME_TICKS = 3600000;
   private IntHashMap deviceQueueMap = new IntHashMap();
   private List<BBacnetCovWorker.WorkerThread> threadPool = new ArrayList<>(this.getNumberOfThreads());
   private static Logger logger = Logger.getLogger("bacnet.cov");

   public int getMaxQueueSize() {
      return this.getInt(maxQueueSize);
   }

   public void setMaxQueueSize(int v) {
      this.setInt(maxQueueSize, v, null);
   }

   public String getWorkerThreadName() {
      return this.getString(workerThreadName);
   }

   public void setWorkerThreadName(String v) {
      this.setString(workerThreadName, v, null);
   }

   public int getNumberOfThreads() {
      return this.getInt(numberOfThreads);
   }

   public void setNumberOfThreads(int v) {
      this.setInt(numberOfThreads, v, null);
   }

   public void removeStaleQueues() {
      this.invoke(removeStaleQueues, null, null);
   }

   public Type getType() {
      return TYPE;
   }

   public void started() {
      this.startThreads();
   }

   public void stopped() {
      this.stopThreads();
   }

   public void atSteadyState() {
      if (this.cleanupTimer == null) {
         this.cleanupTimer = Clock.schedulePeriodically(this, BRelTime.make(3600000L), removeStaleQueues, null);
      }
   }

   public void changed(Property p, Context cx) {
      super.changed(p, cx);
      if (p == numberOfThreads) {
         if (this.isRunning()) {
            logger.fine("Restarting COV Worker threads.");
            this.stopThreads();
            this.startThreads();
         }
      } else if (p == maxQueueSize && this.isRunning()) {
         synchronized (this.deviceQueueMap) {
            Iterator deviceQueue = this.deviceQueueMap.iterator();

            while (deviceQueue.hasNext()) {
               BBacnetCovWorker.LockableQueue oldQueue = (BBacnetCovWorker.LockableQueue)deviceQueue.next();
               BBacnetCovWorker.LockableQueue newQueue = new BBacnetCovWorker.LockableQueue(this.getMaxQueueSize());
               Object[] oldQueueItems = oldQueue.toArray();

               for (int i = 0; i < oldQueueItems.length; i++) {
                  newQueue.enqueue(oldQueueItems[i]);
               }

               this.deviceQueueMap.put(deviceQueue.key(), newQueue);
            }
         }
      }
   }

   private void stopThreads() {
      if (logger.isLoggable(Level.FINE)) {
         logger.fine("Stopping COV threads:\t" + this.threadPool.size());
      }

      synchronized (this.threadPool) {
         for (int i = 0; i < this.threadPool.size(); i++) {
            BBacnetCovWorker.WorkerThread wt = this.threadPool.get(i);
            wt.kill();
            wt.interrupt();
         }

         this.threadPool.clear();
      }
   }

   private void startThreads() {
      if (logger.isLoggable(Level.FINE)) {
         logger.fine("Starting COV threads:\t" + this.getNumberOfThreads() + "\t" + this.getMaxQueueSize());
      }

      synchronized (this.threadPool) {
         int threads = this.getNumberOfThreads();

         for (int i = 0; i < threads; i++) {
            String name = this.getWorkerThreadName();
            if (name.equalsIgnoreCase("")) {
               name = this.getParent().getName() + ":CovWorker";
            }

            BBacnetCovWorker.WorkerThread wt = new BBacnetCovWorker.WorkerThread(name, i);
            this.threadPool.add(wt);
            wt.start();
         }
      }
   }

   public void doRemoveStaleQueues() {
      synchronized (this.deviceQueueMap) {
         Iterator deviceQueue = this.deviceQueueMap.iterator();
         List<Integer> toRemove = new ArrayList<>();

         while (deviceQueue.hasNext()) {
            BBacnetCovWorker.LockableQueue queue = (BBacnetCovWorker.LockableQueue)deviceQueue.next();
            if (queue.isStale()) {
               toRemove.add(deviceQueue.key());
            }
         }

         if (logger.isLoggable(Level.FINE)) {
            logger.fine("Removing stale queues:\t" + toRemove.size());
         }

         for (int i = 0; i < toRemove.size(); i++) {
            int remove = toRemove.get(i);
            if (logger.isLoggable(Level.FINE)) {
               logger.fine("Remaining queuemap:\t" + remove + "\t" + this.deviceQueueMap.size());
            }

            this.deviceQueueMap.remove(remove);
         }
      }
   }

   public void sendCov(Cov cov) {
      BBacnetAddress address = cov.getSub().getRecipient().getRecipient().getAddress();
      BBacnetCovWorker.LockableQueue queue = null;
      synchronized (this.deviceQueueMap) {
         queue = (BBacnetCovWorker.LockableQueue)this.deviceQueueMap.get(address.hash());
         if (queue == null) {
            queue = new BBacnetCovWorker.LockableQueue(this.getMaxQueueSize());
            this.deviceQueueMap.put(address.hash(), queue);
         }
      }

      if (queue != null) {
         boolean queued = false;
         synchronized (queue) {
            try {
               queued = queue.enqueue(cov);
            } catch (QueueFullException var11) {
               ICoalesceable existingCov = (ICoalesceable)queue.find(cov);
               if (existingCov != null) {
                  existingCov.coalesce(cov);
                  queued = true;
               }
            }
         }

         synchronized (this.deviceQueueMap) {
            this.deviceQueueMap.notify();
         }

         if (!queued) {
            logger.severe("Device COV queue full:\t" + address);
         }
      }
   }

   static class LockableQueue extends Queue {
      private long lastAdded;
      private Boolean done = Boolean.TRUE;
      private Object lock = new Object();

      public LockableQueue(int capacity) {
         super(capacity);
         this.lastAdded = Clock.ticks();
      }

      public boolean tryLock() {
         synchronized (this.lock) {
            if (this.done) {
               this.done = Boolean.FALSE;
               return true;
            } else {
               return false;
            }
         }
      }

      public void unlock() {
         synchronized (this.lock) {
            this.done = Boolean.TRUE;
         }
      }

      public boolean enqueue(Object o) {
         this.lastAdded = Clock.ticks();
         return super.enqueue(o);
      }

      public boolean isStale() {
         return this.isEmpty() && Clock.ticks() - this.lastAdded > 3600000L;
      }
   }

   private class WorkerThread extends Thread {
      private boolean alive = true;

      public WorkerThread(String name, int threadId) {
         super(name + threadId);
         this.setDaemon(true);
      }

      public void kill() {
         this.alive = false;
      }

      @Override
      public void run() {
         while (this.alive) {
            BBacnetCovWorker.LockableQueue device = null;

            try {
               device = this.getNextDevice();
               if (device != null) {
                  Runnable r = null;

                  while (true) {
                     synchronized (device) {
                        r = (Runnable)device.dequeue(100);
                     }

                     try {
                        if (r != null) {
                           r.run();
                        }
                     } catch (Exception var14) {
                        BBacnetCovWorker.logger.log(Level.SEVERE, "Exception occurred in WorkerThread runnable", (Throwable)var14);
                     }

                     if (r == null) {
                        break;
                     }
                  }
               } else {
                  synchronized (BBacnetCovWorker.this.deviceQueueMap) {
                     BBacnetCovWorker.this.deviceQueueMap.wait();
                  }
               }
            } catch (InterruptedException var16) {
               break;
            } catch (Exception var17) {
               BBacnetCovWorker.logger.severe("Exception sending notification:" + var17.getMessage());
            } finally {
               if (device != null) {
                  device.unlock();
               }
            }
         }
      }

      public BBacnetCovWorker.LockableQueue getNextDevice() throws InterruptedException {
         BBacnetCovWorker.LockableQueue queue = null;
         synchronized (BBacnetCovWorker.this.deviceQueueMap) {
            if (BBacnetCovWorker.this.deviceQueueMap.isEmpty()) {
               return null;
            } else {
               Iterator deviceQueue = BBacnetCovWorker.this.deviceQueueMap.iterator();

               while (deviceQueue.hasNext()) {
                  queue = (BBacnetCovWorker.LockableQueue)deviceQueue.next();
                  if (!queue.isEmpty() && queue.tryLock()) {
                     return queue;
                  }
               }

               return null;
            }
         }
      }
   }
}
