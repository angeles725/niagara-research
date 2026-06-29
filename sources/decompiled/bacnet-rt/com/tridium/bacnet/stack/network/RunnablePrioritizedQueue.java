package com.tridium.bacnet.stack.network;

import java.util.logging.Level;
import java.util.logging.Logger;

public class RunnablePrioritizedQueue extends PrioritizedQueue implements Runnable {
   private boolean isAlive;
   private Thread thread;
   private static final Logger logger = Logger.getLogger("bacnet.stack");

   public RunnablePrioritizedQueue(int maxSize) {
      super(maxSize);
   }

   public RunnablePrioritizedQueue() {
   }

   public boolean isRunning() {
      return this.thread != null && this.isAlive;
   }

   public void start(String threadName) {
      if (!this.isAlive) {
         this.isAlive = true;
         this.thread = new Thread(this, threadName);
         this.thread.setDaemon(true);
         this.thread.start();
      }
   }

   public void start(ThreadGroup threadGroup, String threadName) {
      if (!this.isAlive) {
         this.isAlive = true;
         this.thread = new Thread(threadGroup, this, threadName);
         this.thread.setDaemon(true);
         this.thread.start();
      }
   }

   public void stop() {
      this.isAlive = false;
      if (this.thread != null) {
         this.thread.interrupt();
      }
   }

   protected int getTimeout() {
      return -1;
   }

   protected void process(PrioritizedQueueEntry entry) {
   }

   @Override
   public void run() {
      this.isAlive = true;

      while (this.isAlive) {
         try {
            this.process(this.dequeue(this.getTimeout()));
         } catch (Throwable var2) {
            logger.log(Level.SEVERE, "Exception occurred while processing RunnablePrioritizedQueue item", var2);
         }
      }
   }
}
