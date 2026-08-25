package javax.baja.nre.util;

import java.util.Arrays;
import java.util.NoSuchElementException;

public class IntHashMap {
   private IntHashMap.Entry[] table;
   private int count;
   private int threshold;
   private float loadFactor;

   public IntHashMap() {
      this(31, 0.75F);
   }

   public IntHashMap(int initialCapacity) {
      this(initialCapacity, 0.75F);
   }

   public IntHashMap(int initialCapacity, float loadFactor) {
      if (initialCapacity > 0 && !(loadFactor <= 0.0)) {
         this.loadFactor = loadFactor;
         this.table = new IntHashMap.Entry[initialCapacity];
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

   public IntHashMap.Iterator iterator() {
      return new IntHashMap.Iterator();
   }

   public Object get(int key) {
      IntHashMap.Entry[] tab = this.table;
      int index = (key & 2147483647) % tab.length;

      for (IntHashMap.Entry e = tab[index]; e != null; e = e.next) {
         if (e.hash == key) {
            return e.value;
         }
      }

      return null;
   }

   public boolean containsKey(int key) {
      IntHashMap.Entry[] tab = this.table;
      int index = (key & 2147483647) % tab.length;

      for (IntHashMap.Entry e = tab[index]; e != null; e = e.next) {
         if (e.hash == key) {
            return true;
         }
      }

      return false;
   }

   private void rehash() {
      int oldCapacity = this.table.length;
      IntHashMap.Entry[] oldTable = this.table;
      int newCapacity = oldCapacity * 2 + 1;
      IntHashMap.Entry[] newTable = new IntHashMap.Entry[newCapacity];
      this.threshold = (int)(newCapacity * this.loadFactor);
      this.table = newTable;
      int i = oldCapacity;

      while (i-- > 0) {
         IntHashMap.Entry old = oldTable[i];

         while (old != null) {
            IntHashMap.Entry e = old;
            old = old.next;
            int index = (e.hash & 2147483647) % newCapacity;
            e.next = newTable[index];
            newTable[index] = e;
         }
      }
   }

   public Object put(int key, Object value) {
      if (value == null) {
         throw new NullPointerException();
      }

      IntHashMap.Entry[] tab = this.table;
      int index = (key & 2147483647) % tab.length;

      for (IntHashMap.Entry e = tab[index]; e != null; e = e.next) {
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
         IntHashMap.Entry e = new IntHashMap.Entry();
         e.hash = key;
         e.value = value;
         e.next = tab[index];
         tab[index] = e;
         this.count++;
         return null;
      }
   }

   public Object remove(int key) {
      IntHashMap.Entry[] tab = this.table;
      int index = (key & 2147483647) % tab.length;
      IntHashMap.Entry e = tab[index];
      IntHashMap.Entry prev = null;

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
      IntHashMap.Entry[] tab = this.table;
      int index = tab.length;

      while (--index >= 0) {
         tab[index] = null;
      }

      this.count = 0;
   }

   @Override
   public boolean equals(Object obj) {
      if (!(obj instanceof IntHashMap)) {
         return false;
      }

      if (this == obj) {
         return true;
      }

      IntHashMap o = (IntHashMap)obj;
      if (this.size() != o.size()) {
         return false;
      }

      for (int i = 0; i < this.table.length; i++) {
         for (IntHashMap.Entry entry = this.table[i]; entry != null; entry = entry.next) {
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
      IntHashMap c = new IntHashMap(this.size() * 3);

      for (int i = 0; i < this.table.length; i++) {
         for (IntHashMap.Entry entry = this.table[i]; entry != null; entry = entry.next) {
            c.put(entry.hash, entry.value);
         }
      }

      return c;
   }

   public Object[] toArray(Object[] a) {
      int nxtIdx = 0;

      for (int i = 0; i < this.table.length; i++) {
         for (IntHashMap.Entry entry = this.table[i]; entry != null && nxtIdx < this.count; entry = entry.next) {
            a[nxtIdx++] = entry.value;
         }
      }

      return a;
   }

   static class Entry {
      int hash;
      Object value;
      IntHashMap.Entry next;

      @Override
      public int hashCode() {
         return this.hash;
      }
   }

   public class Iterator implements java.util.Iterator<Object> {
      private int index = IntHashMap.this.table.length;
      private IntHashMap.Entry entry;
      private int key;

      @Override
      public boolean hasNext() {
         if (this.entry != null) {
            return true;
         }

         while (this.index-- > 0) {
            this.entry = IntHashMap.this.table[this.index];
            if (this.entry != null) {
               return true;
            }
         }

         return false;
      }

      public int key() {
         return this.key;
      }

      @Override
      public Object next() {
         if (this.entry == null) {
            while (this.index-- > 0 && (this.entry = IntHashMap.this.table[this.index]) == null) {
            }
         }

         if (this.entry != null) {
            IntHashMap.Entry e = this.entry;
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
