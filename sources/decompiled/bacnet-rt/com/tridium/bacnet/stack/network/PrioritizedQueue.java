package com.tridium.bacnet.stack.network;

import javax.baja.util.QueueFullException;

public class PrioritizedQueue {
   private PrioritizedQueueEntry head;
   private int size;
   private int maxSize;

   public PrioritizedQueue(int maxSize) {
      this.maxSize = maxSize;
   }

   public PrioritizedQueue() {
      this.maxSize = Integer.MAX_VALUE;
   }

   public synchronized int size() {
      return this.size;
   }

   public int maxSize() {
      return this.maxSize;
   }

   public synchronized boolean isEmpty() {
      return this.head == null;
   }

   public synchronized void enqueue(PrioritizedQueueEntry obj) throws QueueFullException {
      if (this.size >= this.maxSize) {
         throw new QueueFullException();
      } else {
         if (obj != null) {
            if (this.head == null) {
               this.head = obj;
            } else {
               PrioritizedQueueEntry cur = this.head;
               if (obj.getPriority() > cur.getPriority()) {
                  obj.setNext(this.head);
                  this.head = obj;
               } else {
                  while (cur.getNext() != null && cur.getNext().getPriority() >= obj.getPriority()) {
                     cur = cur.getNext();
                  }

                  this.insertAfter(cur, obj);
               }
            }

            this.size++;
         }

         this.notifyAll();
      }
   }

   public synchronized PrioritizedQueueEntry dequeue() {
      return this.dequeue(-1L);
   }

   public synchronized PrioritizedQueueEntry dequeue(long timeout) {
      while (this.isEmpty()) {
         if (timeout == 0L) {
            return null;
         }

         try {
            if (timeout == -1L) {
               this.wait();
            } else {
               this.wait(timeout);
            }
         } catch (InterruptedException var4) {
            return null;
         }

         if (timeout != -1L && this.isEmpty()) {
            return null;
         }
      }

      PrioritizedQueueEntry obj = this.head;
      this.head = obj.getNext();
      obj.setNext(null);
      this.size--;
      this.notifyAll();
      return obj;
   }

   private synchronized void insertAfter(PrioritizedQueueEntry after, PrioritizedQueueEntry msg) {
      msg.setNext(after.getNext());
      after.setNext(msg);
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder("PriQ:" + this.size + " elements:");
      PrioritizedQueueEntry e = this.head;

      for (int i = 0; e != null; e = e.getNext()) {
         sb.append("\n PQE" + i++ + "(" + e.getPriority() + "):" + e);
      }

      return sb.toString();
   }
}
