package com.tridium.json;

import java.util.AbstractList;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map.Entry;

public final class JSONUtil {
   private JSONUtil() {
   }

   public static <T> List<T> toUnmodifiableList(final JSONArray array) {
      return new AbstractList<T>() {
         @Override
         public T get(int index) {
            return (T)array.get(index);
         }

         @Override
         public int size() {
            return array.length();
         }
      };
   }

   public static <K, V> Map<K, V> toUnmodifiableMap(final JSONObject obj) {
      return new AbstractMap<K, V>() {
         @Override
         public Set<Entry<K, V>> entrySet() {
            return new AbstractSet<Entry<K, V>>() {
               @Override
               public Iterator<Entry<K, V>> iterator() {
                  final Iterator<String> keys = obj.keys();
                  return new Iterator<Entry<K, V>>() {
                     @Override
                     public boolean hasNext() {
                        return keys.hasNext();
                     }

                     public Entry<K, V> next() {
                        final String key = keys.next();
                        return new Entry<K, V>() {
                           @Override
                           public K getKey() {
                              return (K)key;
                           }

                           @Override
                           public V getValue() {
                              return (V)obj.get(key);
                           }

                           @Override
                           public V setValue(V value) {
                              throw new UnsupportedOperationException();
                           }
                        };
                     }
                  };
               }

               @Override
               public int size() {
                  return obj.length();
               }
            };
         }
      };
   }

   public static Iterator<String> sortedKeys(JSONObject obj) {
      return new TreeSet<>(obj.keySet()).iterator();
   }

   public static String getString(JSONObject obj, String key) {
      return obj.get(key).toString();
   }

   public static String getString(JSONArray jsonArray, int index) {
      return jsonArray.get(index).toString();
   }
}
