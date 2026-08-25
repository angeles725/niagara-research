package com.tridium.nre.util;

import java.util.LinkedHashMap;
import java.util.Map.Entry;

public class CacheMap<K, V> extends LinkedHashMap<K, V> {
   private final int maxSize;

   public CacheMap(int maxSize, float loadFactor, boolean accessOrder) {
      super(maxSize, loadFactor, accessOrder);
      this.maxSize = maxSize;
   }

   public CacheMap(int maxSize) {
      this(maxSize, 0.75F, true);
   }

   public CacheMap(int maxSize, boolean accessOrder) {
      this(maxSize, 0.75F, accessOrder);
   }

   @Override
   protected boolean removeEldestEntry(Entry<K, V> eldest) {
      return this.size() > this.maxSize;
   }
}
