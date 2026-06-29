package com.tridium.bacnet.stack;

import java.util.EmptyStackException;
import javax.baja.bacnet.BBacnetNetwork;
import javax.baja.bacnet.util.PollList;
import javax.baja.log.Log;
import javax.baja.spy.SpyWriter;

public class MultiPollThread implements Runnable {
   private BBacnetMultiPoll poll;
   private String name;
   private boolean timeToDie;
   private Thread thread;
   private long lastSleep;
   private static final Log logger = Log.getLog("bacnet.point");

   public MultiPollThread(BBacnetMultiPoll poll, String name, int threadNum) {
      this.poll = poll;
      this.name = name + "t" + threadNum;
   }

   public String getName() {
      return this.name;
   }

   public void spyImpl(SpyWriter out) {
      out.prop("  poll port", this.poll.getParent().getName());
      out.prop("  lastSleep", this.lastSleep);
   }

   public void start() {
      this.thread = new Thread(this, this.name);
      this.thread.start();
   }

   public void stop() {
      this.timeToDie = true;
      this.thread.interrupt();
   }

   @Override
   public void run() {
      while (!this.timeToDie) {
         try {
            BBacnetNetwork network = BBacnetNetwork.bacnet();
            if (network == null) {
               return;
            }

            if (network.getEnabled() && this.poll.getPollEnabled()) {
               PollList pl = null;

               try {
                  pl = this.poll.next();
               } catch (EmptyStackException var10) {
                  if (this.poll.dibs != null) {
                     this.poll.dibs.clear();
                     Thread.sleep(1000L);
                  }
               }

               if (pl != null) {
                  try {
                     pl.getDevice().setIsPolling(true);
                     this.poll.poll(pl);
                     pl.pollCount.getAndIncrement();
                  } finally {
                     pl.setIsPolling(false);
                     pl.getDevice().setIsPolling(false);
                  }

                  this.lastSleep = pl.getSleep();
                  if (this.lastSleep > 0L) {
                     Thread.sleep(this.lastSleep);
                  }
               } else {
                  Thread.sleep(1000L);
               }
            } else {
               Thread.sleep(1000L);
            }
         } catch (InterruptedException var11) {
         } catch (Exception var12) {
            logger.error("MultiPollThread runnable exception occurred: ", var12);
         }
      }
   }
}
