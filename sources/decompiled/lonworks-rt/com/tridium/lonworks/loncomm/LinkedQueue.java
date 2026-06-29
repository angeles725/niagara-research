package com.tridium.lonworks.loncomm;

public class LinkedQueue {
   LinkedQueue.Linkable head;
   LinkedQueue.Linkable tail;
   int size;

   public synchronized LinkedQueue.Linkable dequeue() {
      try {
         if (this.size == 0) {
            this.wait();
         }
      } catch (Throwable var2) {
      }

      return this.getNext();
   }

   public synchronized LinkedQueue.Linkable dequeue(int timeout) {
      try {
         if (this.size == 0 && timeout > 0) {
            this.wait(timeout);
         }
      } catch (Throwable var3) {
      }

      return this.getNext();
   }

   private LinkedQueue.Linkable getNext() {
      LinkedQueue.Linkable lnkable = this.head;
      if (lnkable == null) {
         return null;
      } else {
         this.head = lnkable.getNext();
         if (this.head == null) {
            this.tail = null;
         }

         lnkable.setNext(null);
         lnkable.setInQueue(false);
         this.size--;
         return lnkable;
      }
   }

   public synchronized void enqueue(LinkedQueue.Linkable value) {
      if (!value.getInQueue()) {
         value.setNext(null);
         value.setInQueue(true);
         if (this.tail == null) {
            this.head = this.tail = value;
         } else {
            this.tail.setNext(value);
            this.tail = value;
         }

         this.size++;
         this.notifyAll();
      }
   }

   public synchronized void clear() {
      this.size = 0;
      this.head = null;
      this.tail = null;
      this.notifyAll();
   }

   public interface Linkable {
      LinkedQueue.Linkable getNext();

      void setNext(LinkedQueue.Linkable var1);

      default void setInQueue(boolean v) {
      }

      default boolean getInQueue() {
         return false;
      }
   }
}
