package com.tridium.nre;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Predicate;

public class CachedPredicate<T> implements Predicate<T> {
   private final Predicate<T> inner;
   private final Map<T, Boolean> cache;

   public CachedPredicate(Predicate<T> inner) {
      this(inner, 100);
   }

   public CachedPredicate(Predicate<T> inner, final int cacheSize) {
      this.inner = inner;
      this.cache = new LinkedHashMap<T, Boolean>(cacheSize, 0.75F, true) {
         @Override
         public boolean removeEldestEntry(Entry<T, Boolean> eldest) {
            return this.size() > cacheSize;
         }
      };
   }

   @Override
   public boolean test(T t) {
      Boolean result = this.cache.get(t);
      if (result == null) {
         result = this.inner.test(t);
         this.cache.put(t, result);
      }

      return result;
   }

   public void clearCache() {
      this.cache.clear();
   }
}
