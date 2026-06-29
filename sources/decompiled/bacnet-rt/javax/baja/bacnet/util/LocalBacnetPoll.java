package javax.baja.bacnet.util;

import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.nre.util.Array;
import javax.baja.spy.SpyWriter;
import javax.baja.sys.BObject;
import javax.baja.sys.BRelTime;
import javax.baja.sys.Clock;
import javax.baja.sys.Type;

public abstract class LocalBacnetPoll implements Runnable {
   private boolean alive = false;
   private Array<BObject> subs = new Array(BObject.class);
   private Thread thread = null;
   private static final Logger logger = Logger.getLogger("bacnet.util");

   protected LocalBacnetPoll() {
   }

   @Override
   public void run() {
      while (this.alive) {
         long now = Clock.ticks();
         long nextPollTime = now + this.getPollRate().getMillis();
         long sleepTime = nextPollTime - now;
         if (sleepTime > 0L) {
            try {
               Thread.sleep(sleepTime);
            } catch (InterruptedException var10) {
            }
         }

         try {
            for (BObject o : this.subs) {
               if (o != null && o.getType().is(this.getPolledType()) && !this.poll(o)) {
                  this.subs.remove(o);
               }
            }
         } catch (Exception var9) {
            logger.log(Level.SEVERE, "Exception occurred in LocalBacnetPoll runnable", (Throwable)var9);
         }
      }
   }

   protected abstract boolean poll(BObject var1) throws Exception;

   protected abstract BRelTime getPollRate();

   protected abstract String getThreadName();

   protected abstract Type getPolledType();

   public synchronized void subscribe(BObject o) {
      if (o != null) {
         if (!o.getType().is(this.getPolledType())) {
            throw new IllegalArgumentException("wrong type " + o.getType() + " for local poll subscribe (" + this.getPolledType() + ")");
         } else {
            this.subs.add(o);
            if (!this.alive) {
               this.startThread();
            }
         }
      }
   }

   public synchronized void unsubscribe(BObject o) {
      if (o != null) {
         if (!o.getType().is(this.getPolledType())) {
            throw new IllegalArgumentException("wrong type " + o.getType() + " for local poll unsubscribe (" + this.getPolledType() + ")");
         } else {
            this.subs.remove(o);
            if (this.subs.size() == 0) {
               this.stopThread();
            }
         }
      }
   }

   private void startThread() {
      this.alive = true;
      this.thread = new Thread(this, this.getThreadName());
      this.thread.start();
   }

   private void stopThread() {
      this.alive = false;
      this.thread.interrupt();
      this.thread = null;
   }

   public void spy(SpyWriter out) {
      out.prop("alive", this.alive);
      out.trTitle(this.getThreadName(), 2);
      out.prop("subs", this.subs.size());
      Iterator<BObject> it = this.subs.iterator();
      int i = 0;

      while (it.hasNext()) {
         out.prop("  subs[" + i++ + "]", it.next());
      }
   }
}
