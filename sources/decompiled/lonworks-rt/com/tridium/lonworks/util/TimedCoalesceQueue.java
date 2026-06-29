package com.tridium.lonworks.util;

import com.tridium.lonworks.device.DeviceFacets;
import javax.baja.spy.SpyWriter;
import javax.baja.sys.Clock;
import javax.baja.util.ICoalesceable;
import javax.baja.util.QueueFullException;
import javax.baja.util.Worker.ITodo;

public class TimedCoalesceQueue implements ITodo {
   TimedCoalesceQueue.Entry[] table;
   int hashSize;
   int threshold;
   float loadFactor = 0.75F;
   TimedCoalesceQueue.Entry head;
   TimedCoalesceQueue.Entry tail;
   int size;
   int maxSize;
   long fullCount;
   int rehashCnt = 0;

   public TimedCoalesceQueue(int maxSize) {
      this.maxSize = maxSize;
      this.table = new TimedCoalesceQueue.Entry[maxSize / 3];
      this.threshold = (int)(this.table.length * this.loadFactor);
   }

   public TimedCoalesceQueue() {
      this(Integer.MAX_VALUE);
   }

   public int size() {
      return this.size;
   }

   public int maxSize() {
      return this.maxSize;
   }

   public boolean isEmpty() {
      return this.size == 0;
   }

   public boolean isFull() {
      return this.size == this.maxSize;
   }

   public synchronized Object peek() {
      return this.head == null ? null : this.head.value;
   }

   public synchronized Object dequeue() throws InterruptedException {
      if (this.head == null) {
         return null;
      } else {
         long nxtTime = ((TimedCoalesceQueue.ITimed)this.head.value).getTime();

         for (long ticks = Clock.ticks(); nxtTime > ticks; ticks = Clock.ticks()) {
            this.wait(nxtTime - ticks);
            nxtTime = ((TimedCoalesceQueue.ITimed)this.head.value).getTime();
         }

         TimedCoalesceQueue.Entry entry = this.head;
         this.head = entry.next;
         if (this.head == null) {
            this.tail = null;
         }

         entry.next = null;
         this.size--;
         if (entry.value instanceof ICoalesceable) {
            this.remove((ICoalesceable)entry.value);
         }

         return entry.value;
      }
   }

   public synchronized Object dequeue(int timeout) throws InterruptedException {
      while (this.size == 0) {
         if (timeout != -1) {
            this.wait(timeout);
            break;
         }

         this.wait();
      }

      return this.dequeue();
   }

   public synchronized boolean enqueue(TimedCoalesceQueue.ITimed value) throws QueueFullException {
      if (value instanceof ICoalesceable) {
         ICoalesceable newc = (ICoalesceable)value;
         TimedCoalesceQueue.Entry dup = this.get(newc);
         if (dup != null) {
            return false;
         }
      }

      if (this.size >= this.maxSize) {
         this.fullCount++;
         throw new QueueFullException();
      } else if (value == null) {
         throw new NullPointerException();
      } else {
         boolean restart = false;
         TimedCoalesceQueue.Entry entry = this.newEntry(value);
         entry.next = null;
         if (this.tail == null) {
            this.head = this.tail = entry;
            restart = true;
         } else {
            long vTime = value.getTime();
            if (((TimedCoalesceQueue.ITimed)this.tail.value).getTime() <= vTime) {
               this.tail.next = entry;
               this.tail = entry;
            } else if (((TimedCoalesceQueue.ITimed)this.head.value).getTime() > vTime) {
               entry.next = this.head;
               this.head = entry;
               restart = true;
            } else {
               for (TimedCoalesceQueue.Entry e = this.head; e.next != null; e = e.next) {
                  if (((TimedCoalesceQueue.ITimed)e.value).getTime() <= vTime && ((TimedCoalesceQueue.ITimed)e.next.value).getTime() > vTime) {
                     entry.next = e.next;
                     e.next = entry;
                     break;
                  }
               }
            }
         }

         this.size++;
         if (restart) {
            this.notifyAll();
         }

         return true;
      }
   }

   TimedCoalesceQueue.Entry newEntry(TimedCoalesceQueue.ITimed v) {
      return v instanceof ICoalesceable ? this.put((ICoalesceable)v) : new TimedCoalesceQueue.Entry(v);
   }

   public synchronized Object[] toArray() {
      Object[] a = new Object[this.size];
      TimedCoalesceQueue.Entry p = this.head;

      for (int i = 0; p != null; i++) {
         a[i] = p.value;
         p = p.next;
      }

      return a;
   }

   public synchronized void clear() {
      this.size = 0;
      this.head = null;
      this.tail = null;
      this.notifyAll();
   }

   public Runnable todo(int timeout) throws InterruptedException {
      return (Runnable)this.dequeue(timeout);
   }

   TimedCoalesceQueue.Entry get(ICoalesceable c) {
      Object key = c.getCoalesceKey();
      int hash = key.hashCode();
      TimedCoalesceQueue.Entry[] tab = this.table;
      int index = (hash & 2147483647) % tab.length;

      for (TimedCoalesceQueue.Entry e = tab[index]; e != null; e = e.hashNext) {
         if (e.hash == hash && ((ICoalesceable)e.value).getCoalesceKey().equals(key)) {
            return e;
         }
      }

      return null;
   }

   void rehash() {
      int oldCapacity = this.table.length;
      TimedCoalesceQueue.Entry[] oldTable = this.table;
      int newCapacity = oldCapacity * 2 + 1;
      TimedCoalesceQueue.Entry[] newTable = new TimedCoalesceQueue.Entry[newCapacity];
      this.threshold = (int)(newCapacity * this.loadFactor);
      this.table = newTable;
      int i = oldCapacity;

      while (i-- > 0) {
         TimedCoalesceQueue.Entry old = oldTable[i];

         while (old != null) {
            TimedCoalesceQueue.Entry e = old;
            old = old.hashNext;
            int index = (e.hash & 2147483647) % newCapacity;
            e.hashNext = newTable[index];
            newTable[index] = e;
         }
      }

      this.rehashCnt++;
   }

   TimedCoalesceQueue.Entry put(ICoalesceable c) {
      if (this.hashSize >= this.threshold) {
         this.rehash();
         return this.put(c);
      } else {
         Object key = c.getCoalesceKey();
         int hash = key.hashCode();
         TimedCoalesceQueue.Entry[] tab = this.table;
         int index = (hash & 2147483647) % tab.length;
         TimedCoalesceQueue.Entry e = new TimedCoalesceQueue.Entry(c);
         e.hash = hash;
         e.hashNext = tab[index];
         tab[index] = e;
         this.hashSize++;
         return e;
      }
   }

   Object remove(ICoalesceable c) {
      Object key = c.getCoalesceKey();
      int hash = key.hashCode();
      TimedCoalesceQueue.Entry[] tab = this.table;
      int index = (hash & 2147483647) % tab.length;
      TimedCoalesceQueue.Entry e = tab[index];

      for (TimedCoalesceQueue.Entry prev = null; e != null; e = e.hashNext) {
         if (e.hash == hash && ((ICoalesceable)e.value).getCoalesceKey().equals(key)) {
            if (prev != null) {
               prev.hashNext = e.hashNext;
            } else {
               tab[index] = e.hashNext;
            }

            this.hashSize--;
            return e.value;
         }

         prev = e;
      }

      throw new IllegalStateException();
   }

   public synchronized void spy(SpyWriter out) throws Exception {
      out.trTitle("TimedCoalesceQueue", 1);
      out.startProps("Queue");
      out.prop("size", this.size);
      out.prop("maxSize", this.maxSize);
      out.prop("fullCount", Long.toString(this.fullCount));

      for (TimedCoalesceQueue.Entry e = this.head; e != null; e = e.next) {
         DeviceFacets.TimedInvocation ti = (DeviceFacets.TimedInvocation)e.value;
         out.prop(ti.getName(), Long.toString(ti.getTime()));
      }

      out.endProps();
      out.startProps("CoalesceQueue");
      out.prop("hashSize", this.hashSize);
      out.prop("threshold", this.threshold);
      out.prop("rehashCnt", this.rehashCnt);

      for (int i = 0; i < this.table.length; i++) {
         for (TimedCoalesceQueue.Entry var5 = this.table[i]; var5 != null; var5 = var5.hashNext) {
            DeviceFacets.TimedInvocation ti = (DeviceFacets.TimedInvocation)var5.value;
            out.prop(Integer.toString(i, 16), ti.getName());
         }
      }

      out.endProps();
   }

   static class Entry {
      TimedCoalesceQueue.Entry next;
      Object value;
      int hash;
      TimedCoalesceQueue.Entry hashNext;

      Entry(Object v) {
         this.value = v;
      }
   }

   public interface ITimed {
      long getTime();
   }
}
