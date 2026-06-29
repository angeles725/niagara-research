package com.tridium.fox.session;

public class IntMap {
   private IntMap.Entry[] table;
   private int count;
   private int threshold;
   private float loadFactor = 75.0F;

   public IntMap() {
      this.table = new IntMap.Entry[31];
      this.threshold = (int)(31.0F * this.loadFactor);
   }

   public int size() {
      return this.count;
   }

   public Object get(int key) {
      IntMap.Entry[] tab = this.table;
      int index = (key & 2147483647) % tab.length;

      for (IntMap.Entry e = tab[index]; e != null; e = e.next) {
         if (e.hash == key) {
            return e.value;
         }
      }

      return null;
   }

   private void rehash() {
      int oldCapacity = this.table.length;
      IntMap.Entry[] oldTable = this.table;
      int newCapacity = oldCapacity * 2 + 1;
      IntMap.Entry[] newTable = new IntMap.Entry[newCapacity];
      this.threshold = (int)(newCapacity * this.loadFactor);
      this.table = newTable;
      int i = oldCapacity;

      while (i-- > 0) {
         IntMap.Entry old = oldTable[i];

         while (old != null) {
            IntMap.Entry e = old;
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
      } else {
         IntMap.Entry[] tab = this.table;
         int index = (key & 2147483647) % tab.length;

         for (IntMap.Entry e = tab[index]; e != null; e = e.next) {
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
            IntMap.Entry ex = new IntMap.Entry();
            ex.hash = key;
            ex.value = value;
            ex.next = tab[index];
            tab[index] = ex;
            this.count++;
            return null;
         }
      }
   }

   public Object remove(int key) {
      IntMap.Entry[] tab = this.table;
      int index = (key & 2147483647) % tab.length;
      IntMap.Entry e = tab[index];

      for (IntMap.Entry prev = null; e != null; e = e.next) {
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
      }

      return null;
   }

   public void clear() {
      IntMap.Entry[] tab = this.table;
      int index = tab.length;

      while (--index >= 0) {
         tab[index] = null;
      }

      this.count = 0;
   }

   public Object[] toArray(Object[] a) {
      int nxtIdx = 0;

      for (int i = 0; i < this.table.length; i++) {
         for (IntMap.Entry entry = this.table[i]; entry != null && nxtIdx < this.count; entry = entry.next) {
            a[nxtIdx++] = entry.value;
         }
      }

      return a;
   }

   static class Entry {
      int hash;
      Object value;
      IntMap.Entry next;
   }
}
