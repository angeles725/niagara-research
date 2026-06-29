package com.tridium.bacnet.timers;

import javax.baja.nre.util.IntHashMap;
import javax.baja.nre.util.IntHashMap.Iterator;
import javax.baja.spy.SpyWriter;
import javax.baja.sys.Clock;

public class Timers implements Runnable {
   public static final int INVALID_TIMER_ID = -1;
   public static final long CANCEL_TIMER = 0L;
   private IntHashMap timerTable = new IntHashMap();
   private int nextTimerId = 0;
   private boolean alive = false;
   private long nextDeadline = Long.MAX_VALUE;
   private Thread myThread = new Thread(this, "BnTmrs");
   private static Timers timers;

   public static void start() {
      if (timers == null) {
         timers = new Timers();
      }
   }

   public static void stop() {
      if (timers != null) {
         synchronized (timers) {
            if (timers.myThread != null) {
               try {
                  timers.myThread.interrupt();
               } catch (Exception var3) {
               }

               timers.myThread = null;
               timers.timerTable = null;
            }
         }
      }

      timers = null;
   }

   public static int add(TimerListener listener, long msec) {
      return timers.addTimer(listener, msec, null);
   }

   public static int add(TimerListener listener, long msec, Object object) {
      return timers.addTimer(listener, msec, object);
   }

   public static void cancel(TimerListener listener) {
      timers.cancelTimer(listener);
   }

   public static Object cancel(int timerId) {
      return timers.cancelTimer(timerId);
   }

   public static synchronized Object reset(int timerId, long msec) {
      return timers.resetTimer(timerId, msec);
   }

   public static void spy(SpyWriter out) throws Exception {
      out.startProps();
      out.trTitle("Timers", 2);
      synchronized (timers) {
         out.prop("nextTimerId", timers.nextTimerId);
         out.prop("alive", timers.alive);
         out.prop("nextDeadline", "" + timers.nextDeadline);
         out.prop("timerTable size", timers.timerTable.size());
         Iterator it = timers.timerTable.iterator();

         while (it.hasNext()) {
            Timers.TimerToken mgr = (Timers.TimerToken)it.next();
            out.prop("" + it.key(), mgr.userObject);
         }
      }

      out.endProps();
   }

   private Timers() {
      this.myThread.start();
   }

   private int addTimer(TimerListener listener, long msec, Object object) {
      Timers.TimerToken newTimer = null;
      long deadline = Clock.ticks() + msec;
      synchronized (this) {
         if (this.nextTimerId++ == -1) {
            this.nextTimerId++;
         }

         newTimer = new Timers.TimerToken(deadline, this.nextTimerId, listener, object);
         this.timerTable.put(newTimer.timerId, newTimer);
         if (newTimer.deadline < this.nextDeadline) {
            this.nextDeadline = newTimer.deadline;
         }

         this.notifyAll();
      }

      return newTimer.timerId;
   }

   private void cancelTimer(TimerListener listener) {
      synchronized (this) {
         Iterator timerList = this.timerTable.iterator();

         while (timerList.hasNext()) {
            Timers.TimerToken t = (Timers.TimerToken)timerList.next();
            if (listener == t.listener) {
               this.timerTable.remove(t.timerId);
            }
         }
      }
   }

   private Object cancelTimer(int timerId) {
      synchronized (this) {
         Timers.TimerToken t = (Timers.TimerToken)this.timerTable.get(timerId);
         if (t == null) {
            return null;
         } else {
            this.timerTable.remove(timerId);
            return t.userObject;
         }
      }
   }

   private synchronized Object resetTimer(int timerId, long msec) {
      Timers.TimerToken t = (Timers.TimerToken)this.timerTable.get(timerId);
      if (t == null) {
         return null;
      } else {
         long newDeadline = Clock.ticks() + msec;
         t.deadline = newDeadline;
         return t.userObject;
      }
   }

   @Override
   public void run() {
      for (this.alive = true; this.alive; this.notifyListeners()) {
         try {
            synchronized (this) {
               while (this.timerTable != null && this.timerTable.size() < 1) {
                  this.wait();
               }

               long sleepTime = this.nextDeadline - Clock.ticks();
               if (sleepTime > 0L) {
                  this.wait(sleepTime);
               }
            }
         } catch (InterruptedException var6) {
            break;
         }
      }
   }

   private synchronized void notifyListeners() {
      long now = Clock.ticks();
      this.nextDeadline = Long.MAX_VALUE;
      long restartTime = 0L;
      if (this.timerTable != null) {
         Iterator timerList = this.timerTable.iterator();

         while (timerList.hasNext()) {
            Timers.TimerToken t = (Timers.TimerToken)timerList.next();
            if (t.deadline <= now) {
               restartTime = t.listener.timerExpired(t.timerId, t.userObject);
               if (restartTime == 0L) {
                  this.timerTable.remove(t.timerId);
               } else {
                  t.deadline = Clock.ticks() + restartTime;
                  if (t.deadline < this.nextDeadline) {
                     this.nextDeadline = t.deadline;
                  }
               }
            } else if (t.deadline < this.nextDeadline) {
               this.nextDeadline = t.deadline;
            }
         }
      }
   }

   static class TimerToken {
      public long deadline;
      public int timerId;
      public TimerListener listener;
      public Object userObject;

      public TimerToken(long deadline, int timerId, TimerListener listener, Object userObject) {
         this.deadline = deadline;
         this.timerId = timerId;
         this.listener = listener;
         this.userObject = userObject;
      }
   }
}
