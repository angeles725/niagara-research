package com.tridium.fox.session;

public final class FrameQueue {
   private FoxFrame head;
   private FoxFrame tail;
   private int size;
   private int peak;
   private String blocked;
   private boolean isAlive = true;

   public final int size() {
      return this.size;
   }

   public final int peak() {
      return this.peak;
   }

   public final int max() {
      return Fox.maxQueueSize;
   }

   public final synchronized void kill() {
      this.isAlive = false;
      this.notifyAll();
   }

   public synchronized FoxFrame dequeue(int timeout) throws InterruptedException {
      while (this.isAlive && this.size == 0) {
         if (timeout != -1) {
            this.wait(timeout);
            break;
         }

         this.wait();
      }

      FoxFrame f = this.head;
      if (f == null) {
         return null;
      } else {
         this.head = f.next;
         if (this.head == null) {
            this.tail = null;
         }

         f.next = null;
         this.size--;
         this.notifyAll();
         return f;
      }
   }

   public synchronized void enqueue(FoxFrame f) throws InterruptedException {
      while (this.isAlive && this.size >= this.max()) {
         try {
            this.blocked = Thread.currentThread().getName();
         } catch (Exception var3) {
         }

         this.wait();
      }

      this.blocked = null;
      if (f.next != null) {
         throw new IllegalStateException();
      } else {
         if (this.tail == null) {
            this.head = this.tail = f;
         } else {
            this.tail.next = f;
            this.tail = f;
         }

         this.size++;
         if (this.size > this.peak) {
            this.peak = this.size;
         }

         this.notifyAll();
      }
   }

   public synchronized void clear() {
      this.size = 0;
      this.head = null;
      this.tail = null;
      this.notifyAll();
   }

   @Override
   public String toString() {
      String s = "FrameQueue size=" + this.size + " peak=" + this.peak + " max=" + this.max();
      if (this.blocked != null) {
         s = s + " blocked=" + this.blocked;
      }

      return s;
   }
}
