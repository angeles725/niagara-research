package com.tridium.fox.sys;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import javax.baja.fox.BFoxProxySession;
import javax.baja.sys.Clock;

public class ActivityMonitor {
   private final long resetTimeoutInterval;
   private final long notifyInterval;
   private final BFoxSession session;
   private final Set<BFoxProxySession.NotifyListener> notifyListeners = Collections.newSetFromMap(new WeakHashMap<>());
   private final Set<Object> pauseSet = Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));
   private final ScheduledExecutorService executor;
   private long lastActivity;
   private boolean inWarningPeriod;
   private boolean ignoreActivity;
   private ScheduledFuture<?> resetTimeoutFuture;
   private ScheduledFuture<?> notifyTimeoutFuture;

   public ActivityMonitor(long resetTimeoutInterval, long notifyInterval, BFoxSession session, ScheduledExecutorService executor) {
      this.resetTimeoutInterval = resetTimeoutInterval;
      this.notifyInterval = notifyInterval;
      this.session = session;
      this.executor = executor;
   }

   public void start() {
      this.lastActivity = Clock.ticks();
      this.resetTimeoutFuture = this.executor
         .scheduleAtFixedRate(new ActivityMonitor.ResetTimeoutRunnable(), 0L, this.resetTimeoutInterval, TimeUnit.MILLISECONDS);
   }

   public void stop() {
      if (this.notifyTimeoutFuture != null) {
         this.notifyTimeoutFuture.cancel(false);
      }

      if (this.resetTimeoutFuture != null) {
         this.resetTimeoutFuture.cancel(false);
      }
   }

   public void activity() {
      if (!this.ignoreActivity) {
         this.lastActivity = Clock.ticks();
         if (this.inWarningPeriod) {
            this.inWarningPeriod = false;
            this.resetSessionTimeout();
         }
      }
   }

   public Object pause() {
      Object token = new Object();
      this.pauseSet.add(token);
      return token;
   }

   public void resume(Object token) {
      this.pauseSet.remove(token);
      if (this.pauseSet.isEmpty()) {
         this.activity();
      }
   }

   public void addNotifyListener(BFoxProxySession.NotifyListener listener) {
      this.notifyListeners.add(listener);
   }

   public void removeNotifyListener(BFoxProxySession.NotifyListener listener) {
      this.notifyListeners.remove(listener);
   }

   private void resetSessionTimeout() {
      long offset = 0L;
      if (this.pauseSet.isEmpty()) {
         long now = Clock.ticks();
         offset = now - this.lastActivity;
      }

      try {
         long timeout = this.session.resetSessionTimeout(offset);
         if (timeout > -1L) {
            if (this.notifyTimeoutFuture != null) {
               this.notifyTimeoutFuture.cancel(false);
            }

            this.notifyTimeoutFuture = this.executor
               .schedule(new ActivityMonitor.NotifyTimeoutRunnable(), Math.max(timeout - this.notifyInterval, 0L), TimeUnit.MILLISECONDS);
         }
      } catch (Exception var5) {
      }
   }

   private class NotifyTimeoutRunnable implements Runnable {
      private NotifyTimeoutRunnable() {
      }

      @Override
      public void run() {
         long offset = 0L;
         if (ActivityMonitor.this.pauseSet.isEmpty()) {
            long now = Clock.ticks();
            offset = now - ActivityMonitor.this.lastActivity;
         }

         long timeout = -1L;

         try {
            timeout = ActivityMonitor.this.session.resetSessionTimeout(offset);
         } catch (Exception var7) {
         }

         if (timeout <= ActivityMonitor.this.notifyInterval && timeout > -1L) {
            ActivityMonitor.this.inWarningPeriod = true;
            if (!ActivityMonitor.this.notifyListeners.isEmpty()) {
               BFoxProxySession.NotifyListener listener = ActivityMonitor.this.notifyListeners.iterator().next();
               if (listener != null) {
                  ActivityMonitor.this.ignoreActivity = true;
                  boolean shouldReset = listener.onNotify(ActivityMonitor.this.session);
                  ActivityMonitor.this.ignoreActivity = false;
                  if (shouldReset) {
                     ActivityMonitor.this.activity();
                  }
               }
            }
         } else if (timeout > -1L) {
            ActivityMonitor.this.notifyTimeoutFuture = ActivityMonitor.this.executor
               .schedule(ActivityMonitor.this.new NotifyTimeoutRunnable(), Math.max(timeout - ActivityMonitor.this.notifyInterval, 0L), TimeUnit.MILLISECONDS);
         }
      }
   }

   private class ResetTimeoutRunnable implements Runnable {
      private ResetTimeoutRunnable() {
      }

      @Override
      public void run() {
         ActivityMonitor.this.resetSessionTimeout();
      }
   }
}
