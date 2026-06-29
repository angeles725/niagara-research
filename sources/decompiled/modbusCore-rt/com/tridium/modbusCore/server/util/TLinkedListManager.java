package com.tridium.modbusCore.server.util;

import java.util.Enumeration;
import java.util.NoSuchElementException;

public class TLinkedListManager {
   private TLinkedListElement head;
   private TLinkedListElement tail;
   private int size;
   private String name;

   public TLinkedListManager(String name) {
      this.name = name;
   }

   public synchronized int size() {
      return this.size;
   }

   public synchronized boolean isEmpty() {
      return this.head == null;
   }

   public synchronized void signal() {
      this.notifyAll();
   }

   public synchronized void addToHead(TLinkedListElement linkage) {
      TLinkedListManager mgr = linkage.getLinkedListManager();
      if (mgr != null) {
         throw new IllegalStateException("Already linked " + mgr.getName() + ", my name " + this.getName());
      } else {
         linkage.setLinkedListManager(this);
         if (this.head == null) {
            this.head = this.tail = linkage;
         } else {
            linkage.setNext(this.head);
            this.head = linkage;
         }

         this.size++;
         this.notifyAll();
      }
   }

   public synchronized void addToTail(TLinkedListElement linkage) {
      TLinkedListManager mgr = linkage.getLinkedListManager();
      if (mgr != null) {
         throw new IllegalStateException("Already linked " + mgr.getName() + ", my name " + this.getName());
      } else {
         linkage.setLinkedListManager(this);
         if (this.head == null) {
            this.head = this.tail = linkage;
         } else {
            this.tail.setNext(linkage);
            this.tail = linkage;
         }

         this.size++;
         this.notifyAll();
      }
   }

   public synchronized TLinkedListElement removeFromHead() {
      if (this.head == null) {
         return null;
      } else {
         TLinkedListElement linkage = this.head;
         this.head = linkage.getNext();
         if (this.head == null) {
            this.tail = null;
         }

         this.size--;
         linkage.setLinkedListManager(null);
         linkage.setNext(null);
         return linkage;
      }
   }

   public synchronized TLinkedListElement removeFromHead(long timeout) throws InterruptedException {
      if (this.head == null) {
         if (timeout == -1L) {
            this.wait();
         } else {
            this.wait(timeout);
         }
      }

      return this.removeFromHead();
   }

   public String getName() {
      return this.name;
   }

   @Override
   public String toString() {
      StringBuilder buf = new StringBuilder();
      Enumeration<TLinkedListElement> e = this.elements();
      boolean first = true;
      buf.append(this.size()).append(" {");

      for (; e.hasMoreElements(); buf.append(e.nextElement())) {
         if (first) {
            first = false;
         } else {
            buf.append(", ");
         }
      }

      buf.append("}");
      return buf.toString();
   }

   public Enumeration<TLinkedListElement> elements() {
      return new TLinkedListManager.Enumerator();
   }

   public class Enumerator implements Enumeration<TLinkedListElement> {
      private TLinkedListElement cur = TLinkedListManager.this.head;

      @Override
      public boolean hasMoreElements() {
         return this.cur != null;
      }

      public TLinkedListElement nextElement() {
         if (this.cur == null) {
            throw new NoSuchElementException();
         } else {
            TLinkedListElement p = this.cur;
            this.cur = this.cur.getNext();
            return p;
         }
      }
   }
}
