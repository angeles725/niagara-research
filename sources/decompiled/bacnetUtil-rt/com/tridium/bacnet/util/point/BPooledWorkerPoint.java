package com.tridium.bacnet.util.point;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.NotRunningException;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public abstract class BPooledWorkerPoint extends BPeriodicNumericPoint {
   public static final Type TYPE = Sys.loadType(BPooledWorkerPoint.class);
   private static Object POOL_LOCK = new Object();
   private static ExecutorService pool;

   @Override
   public Type getType() {
      return TYPE;
   }

   @Override
   public void started() throws Exception {
      try {
         super.started();
      } catch (NotRunningException var4) {
      }

      synchronized (POOL_LOCK) {
         if (pool == null) {
            AccessController.doPrivileged((PrivilegedAction<Void>)(() -> {
               pool = Executors.newSingleThreadExecutor(new ThreadFactory() {
                  @Override
                  public Thread newThread(Runnable r) {
                     Thread t = Executors.defaultThreadFactory().newThread(r);
                     t.setDaemon(true);
                     return t;
                  }
               });
               return null;
            }));
         }
      }
   }

   @Override
   public void stopped() throws Exception {
      super.stopped();
      synchronized (POOL_LOCK) {
         if (pool != null) {
            pool.shutdownNow();
         }

         pool = null;
      }
   }

   public void run(Runnable runnable) {
      if (pool != null) {
         pool.execute(runnable);
      }
   }
}
