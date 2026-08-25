package javax.baja.nre.util;

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Objects;

public class LongHashMap {
   private LongHashMap.Entry[] table;
   private int count;
   private int threshold;
   private float loadFactor;

   public LongHashMap() {
      this(31, 0.75F);
   }

   public LongHashMap(int initialCapacity) {
      this(initialCapacity, 0.75F);
   }

   public LongHashMap(int initialCapacity, float loadFactor) {
      if (initialCapacity > 0 && !(loadFactor <= 0.0)) {
         this.loadFactor = loadFactor;
         this.table = new LongHashMap.Entry[initialCapacity];
         this.threshold = (int)(initialCapacity * loadFactor);
      } else {
         throw new IllegalArgumentException();
      }
   }

   public int size() {
      return this.count;
   }

   public boolean isEmpty() {
      return this.count == 0;
   }

   public LongHashMap.Iterator iterator() {
      return new LongHashMap.Iterator();
   }

   public Object get(long key) {
      LongHashMap.Entry[] tab = this.table;
      int index = (this.hash(key) & 2147483647) % tab.length;

      for (LongHashMap.Entry e = tab[index]; e != null; e = e.next) {
         if (e.hash == key) {
            return e.value;
         }
      }

      return null;
   }

   private void rehash() {
      int oldCapacity = this.table.length;
      LongHashMap.Entry[] oldTable = this.table;
      int newCapacity = oldCapacity * 2 + 1;
      LongHashMap.Entry[] newTable = new LongHashMap.Entry[newCapacity];
      this.threshold = (int)(newCapacity * this.loadFactor);
      this.table = newTable;
      int i = oldCapacity;

      while (i-- > 0) {
         LongHashMap.Entry old = oldTable[i];

         while (old != null) {
            LongHashMap.Entry e = old;
            old = old.next;
            int index = (this.hash(e.hash) & 2147483647) % newCapacity;
            e.next = newTable[index];
            newTable[index] = e;
         }
      }
   }

   public Object put(long key, Object value) {
      if (value == null) {
         throw new NullPointerException();
      }

      LongHashMap.Entry[] tab = this.table;
      int index = (this.hash(key) & 2147483647) % tab.length;

      for (LongHashMap.Entry e = tab[index]; e != null; e = e.next) {
         if (e.hash == key) {
            Object old = e.value;
            e.value = value;
            return old;
         }
      }

      if (this.count >= this.threshold) {
         this.rehash();
         return this.put(key, value);
      } else {
         LongHashMap.Entry e = new LongHashMap.Entry();
         e.hash = key;
         e.value = value;
         e.next = tab[index];
         tab[index] = e;
         this.count++;
         return null;
      }
   }

   public Object remove(long key) {
      LongHashMap.Entry[] tab = this.table;
      int index = (this.hash(key) & 2147483647) % tab.length;
      LongHashMap.Entry e = tab[index];
      LongHashMap.Entry prev = null;

      while (e != null) {
         if (e.hash == key) {
            if (prev != null) {
               prev.next = e.next;
            } else {
               tab[index] = e.next;
            }

            this.count--;
            return e.value;
         }

         prev = e;
         e = e.next;
      }

      return null;
   }

   public void clear() {
      LongHashMap.Entry[] tab = this.table;
      int index = tab.length;

      while (--index >= 0) {
         tab[index] = null;
      }

      this.count = 0;
   }

   @Override
   public boolean equals(Object obj) {
      if (!(obj instanceof LongHashMap)) {
         return false;
      }

      if (this == obj) {
         return true;
      }

      LongHashMap o = (LongHashMap)obj;

      for (int i = 0; i < this.table.length; i++) {
         for (LongHashMap.Entry entry = this.table[i]; entry != null; entry = entry.next) {
            Object tv = entry.value;
            Object ov = o.get(entry.hash);
            if (ov == null || !tv.equals(ov)) {
               return false;
            }
         }
      }

      return true;
   }

   @Override
   public int hashCode() {
      return Arrays.hashCode(this.table);
   }

   @Override
   public Object clone() {
      LongHashMap c = new LongHashMap(this.size() * 3);

      for (int i = 0; i < this.table.length; i++) {
         for (LongHashMap.Entry entry = this.table[i]; entry != null; entry = entry.next) {
            c.put(entry.hash, entry.value);
         }
      }

      return c;
   }

   public Object[] toArray(Object[] a) {
      int nxtIdx = 0;

      for (int i = 0; i < this.table.length; i++) {
         for (LongHashMap.Entry entry = this.table[i]; entry != null && nxtIdx < this.count; entry = entry.next) {
            a[nxtIdx++] = entry.value;
         }
      }

      return a;
   }

   private int hash(long x) {
      return (int)(x ^ x >> 32);
   }

   static class Entry {
      long hash;
      Object value;
      LongHashMap.Entry next;

      @Override
      public int hashCode() {
         return Objects.hashCode(this.hash);
      }
   }

   public class Iterator implements java.util.Iterator<Object> {
      private int index = LongHashMap.this.table.length;
      private LongHashMap.Entry entry;
      private long key;

      @Override
      public boolean hasNext() {
         if (this.entry != null) {
            return true;
         }

         while (this.index-- > 0) {
            this.entry = LongHashMap.this.table[this.index];
            if (this.entry != null) {
               return true;
            }
         }

         return false;
      }

      public long key() {
         return this.key;
      }

      @Override
      public Object next() {
         if (this.entry == null) {
            while (this.index-- > 0 && (this.entry = LongHashMap.this.table[this.index]) == null) {
            }
         }

         if (this.entry != null) {
            LongHashMap.Entry e = this.entry;
            this.entry = e.next;
            this.key = e.hash;
            return e.value;
         } else {
            throw new NoSuchElementException();
         }
      }

      @Override
      public void remove() {
         throw new UnsupportedOperationException();
      }
   }
}
