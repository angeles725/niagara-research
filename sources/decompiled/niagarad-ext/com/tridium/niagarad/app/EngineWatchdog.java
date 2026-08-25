package com.tridium.niagarad.app;

import com.tridium.niagarad.NiagaraDaemon;
import com.tridium.nre.platform.IPlatformProvider;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

class EngineWatchdog {
   protected Logger filter = null;
   protected App app = null;
   private EngineWatchdog.EngineWatchdogThread watchdogThread = null;
   private final IPlatformProvider platformProvider;

   public EngineWatchdog(App toWatch, Logger filter, IPlatformProvider platformProvider) {
      this.platformProvider = platformProvider;
      this.app = toWatch;
      this.filter = filter;
   }

   int threadWaitForStop(int seconds) {
      try {
         Thread.sleep(seconds * 1000L);
      } catch (InterruptedException e) {
         if (this.watchdogThread.stopRequested) {
            this.watchdogThread.stopRequested = false;
            return -1;
         }
      }

      return 0;
   }

   public int init() {
      return this.platformProvider.createWatchdog(this.app.getAppName());
   }

   public boolean isAlive() {
      return this.watchdogThread != null && this.watchdogThread.isAlive();
   }

   public void start() {
      if (this.watchdogThread != null) {
         this.filter.warning("watchdog thread already initialized, not creating new thread");
      } else {
         this.watchdogThread = new EngineWatchdog.EngineWatchdogThread();
         this.watchdogThread.start();
         this.filter.info(this.app.getAppType() + " " + this.app.getAppName() + " watchdog thread started [tid = " + this.watchdogThread.getId() + "]");
      }
   }

   public void stop() {
      if (this.watchdogThread != null) {
         if (this.watchdogThread.isAlive()) {
            if (this.filter.isLoggable(Level.FINE)) {
               this.filter.fine("stopping watchdog thread for " + this.app.getAppType() + " " + this.app.getAppName());
            }

            this.watchdogThread.stopRequested = true;
            this.watchdogThread.interrupt();

            try {
               this.watchdogThread.join();
            } catch (InterruptedException var2) {
            }

            this.watchdogThread.stopRequested = false;
            if (this.filter.isLoggable(Level.FINE)) {
               this.filter.fine("watchdog thread for " + this.app.getAppType() + " " + this.app.getAppName() + " stopped");
            }
         } else if (this.filter.isLoggable(Level.FINE)) {
            this.filter.fine("engine watchdog thread was already stopped for " + this.app.getAppType() + " " + this.app.getAppName() + ", cleaning up");
         }

         this.watchdogThread = null;
         this.platformProvider.updateWatchdog(this.app.getAppName(), 0, -1, 1);
      }
   }

   public void run() {
      this.platformProvider.updateWatchdog(this.app.getAppName(), 0, -1, 1);
      int lastCycles = -1;
      int sleepTime = 1;
      WatchdogShmem shmem = new WatchdogShmem();
      boolean done = false;
      this.watchdogThread.stopRequested = false;

      while (!done) {
         if (this.threadWaitForStop(sleepTime) != 0) {
            this.watchdogThread.stopRequested = false;
            break;
         }

         shmem.engineCycles = this.platformProvider.getWatchdogCycles(this.app.getAppName());
         shmem.policy = this.platformProvider.getWatchdogPolicy(this.app.getAppName());
         shmem.timeout = this.platformProvider.getWatchdogTimeout(this.app.getAppName());
         sleepTime = shmem.timeout;
         if (sleepTime <= 0) {
            sleepTime = 1;
         }

         if (shmem.policy != -1 && shmem.timeout > 0) {
            if (this.filter.isLoggable(Level.FINE)) {
               StringBuilder buffer = new StringBuilder();
               buffer.append("WATCHDOG CHECK ")
                  .append(this.app.getAppType())
                  .append(" ")
                  .append(this.app.getAppName())
                  .append(" end wd last cycles ")
                  .append(lastCycles)
                  .append(" cycles ")
                  .append(shmem.engineCycles);
               this.filter.fine(buffer.toString());
            }

            if (lastCycles != shmem.engineCycles) {
               lastCycles = shmem.engineCycles;
            } else {
               StringBuilder buffer = new StringBuilder();
               buffer.append(this.app.getAppType()).append(" ").append(this.app.getAppName()).append(" ENGINE LOCKUP DETECTED");
               this.filter.warning(buffer.toString());
               done = true;
               long now = System.currentTimeMillis();
               buffer = new StringBuilder();
               buffer.append("ENGINE WATCHDOG TIMEOUT STACK DUMP @ ").append(new Date(now));
               this.app.getAppOut().printf(buffer.toString().getBytes(StandardCharsets.UTF_8));
               this.app.generateStackDump();
               if (shmem.policy != 1) {
                  try {
                     Thread.sleep(15000L);
                  } catch (InterruptedException e) {
                     this.watchdogThread.stopRequested = false;
                  }
               }

               switch (shmem.policy) {
                  case 1:
                     buffer = new StringBuilder();
                     buffer.append(this.app.getAppType()).append(" ").append(this.app.getAppName()).append(" ENGINE WATCHDOG - LOGGING ONLY");
                     this.filter.warning(buffer.toString());
                     break;
                  case 2:
                  default:
                     buffer = new StringBuilder();
                     buffer.append(this.app.getAppType()).append(" ").append(this.app.getAppName()).append(" ENGINE WATCHDOG - KILLING VM");
                     this.filter.warning(buffer.toString());
                     this.app.kill(0);
                     break;
                  case 3:
                     buffer = new StringBuilder();
                     buffer.append(this.app.getAppType()).append(" ").append(this.app.getAppName()).append(" ENGINE WATCHDOG - REBOOTING PLATFORM");
                     this.filter.warning(buffer.toString());
                     this.app.kill(2);
                     NiagaraDaemon.getInstance().queueReboot();
               }
            }
         }
      }
   }

   private class EngineWatchdogThread extends Thread {
      boolean stopRequested = false;

      public EngineWatchdogThread() {
         super("Niagarad:WatchdogThread(" + EngineWatchdog.this.app.getAppName() + ")");
      }

      @Override
      public void run() {
         EngineWatchdog.this.run();
      }
   }
}
