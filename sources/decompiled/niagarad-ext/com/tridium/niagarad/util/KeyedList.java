package com.tridium.niagarad.util;

import java.util.ArrayList;

public class KeyedList {
   private final Object listMonitor = new Object();
   private ArrayList<KeyedList.Tuple> attrs = null;

   public void set(String key, String value) {
      synchronized (this.listMonitor) {
         if (this.attrs == null) {
            this.add(key, value);
         } else {
            for (int i = 0; i < this.attrs.size(); i++) {
               KeyedList.Tuple current = this.attrs.get(i);
               if (current.key.equalsIgnoreCase(key)) {
                  this.attrs.set(i, new KeyedList.Tuple(key, value));
                  return;
               }
            }

            this.add(key, value);
         }
      }
   }

   public void add(String key, String value) {
      synchronized (this.listMonitor) {
         if (this.attrs == null) {
            this.attrs = new ArrayList<>();
         }

         this.attrs.add(new KeyedList.Tuple(key, value));
      }
   }

   public void addAll(KeyedList other) {
      if (other != null) {
         for (int i = 0; i < other.size(); i++) {
            this.add(other.getKey(i), other.getAtIndex(i));
         }
      }
   }

   public int size() {
      synchronized (this.listMonitor) {
         return this.attrs == null ? 0 : this.attrs.size();
      }
   }

   public String get(String key, String defaultValue) {
      synchronized (this.listMonitor) {
         if (this.attrs == null) {
            return defaultValue;
         }

         for (KeyedList.Tuple attr : this.attrs) {
            if (attr.key.equalsIgnoreCase(key)) {
               return attr.value;
            }
         }

         return defaultValue;
      }
   }

   public String getKey(int index) {
      synchronized (this.listMonitor) {
         if (this.attrs == null) {
            return null;
         } else {
            return this.attrs.size() <= index ? null : this.attrs.get(index).key;
         }
      }
   }

   public String getAtIndex(int index) {
      synchronized (this.listMonitor) {
         if (this.attrs == null) {
            return null;
         } else {
            return this.attrs.size() <= index ? null : this.attrs.get(index).value;
         }
      }
   }

   public boolean containsKey(String key) {
      synchronized (this.listMonitor) {
         if (this.attrs == null) {
            return false;
         }

         for (KeyedList.Tuple attr : this.attrs) {
            if (attr.key.equalsIgnoreCase(key)) {
               return true;
            }
         }

         return false;
      }
   }

   public boolean removeAll(String key) {
      boolean found = false;
      if (key == null) {
         return false;
      }

      synchronized (this.listMonitor) {
         if (this.attrs == null) {
            return false;
         }

         for (int i = 0; i < this.attrs.size(); i++) {
            KeyedList.Tuple current = this.attrs.get(i);
            if (current.key.equalsIgnoreCase(key)) {
               found = true;
               this.attrs.remove(i);
               i--;
            }
         }

         return found;
      }
   }

   public KeyedList duplicateDeep() {
      KeyedList copy = new KeyedList();
      synchronized (this.listMonitor) {
         for (KeyedList.Tuple attr : this.attrs) {
            KeyedList.Tuple current = attr;
            copy.add(current.key, current.value);
         }

         return copy;
      }
   }

   static class Tuple {
      public String key = null;
      public String value = null;

      public Tuple(String key, String value) {
         this.key = key;
         this.value = value;
      }
   }
}
