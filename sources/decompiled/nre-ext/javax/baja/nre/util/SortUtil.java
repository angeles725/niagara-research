package javax.baja.nre.util;

import java.util.Comparator;

public class SortUtil {
   public static final Comparator<? super Object> ASCENDING = SortUtil::compare;
   public static final Comparator<? super Object> DESCENDING = ASCENDING.reversed();

   public static <T> void sort(T[] values) {
      sort(values, values, ASCENDING);
   }

   public static <T> void rsort(T[] values) {
      sort(values, values, DESCENDING);
   }

   public static <K, V> void sort(K[] keys, V[] values) {
      sort(keys, values, ASCENDING);
   }

   public static <K, V> void rsort(K[] keys, V[] values) {
      sort(keys, values, DESCENDING);
   }

   public static <K, V> void sort(K[] keys, V[] values, boolean ascending) {
      if (ascending) {
         sort(keys, values, ASCENDING);
      } else {
         sort(keys, values, DESCENDING);
      }
   }

   public static <K, V> void sort(K[] keys, V[] values, Comparator<? super K> comparator) {
      if (keys.length != values.length) {
         throw new IllegalArgumentException("keys.length != values.length");
      }

      int n = keys.length;

      for (int incr = n / 2; incr >= 1; incr /= 2) {
         for (int i = incr; i < n; i++) {
            K tempKey = keys[i];
            V tempValue = values[i];

            int j;
            for (j = i; j >= incr && comparator.compare(tempKey, keys[j - incr]) < 0; j -= incr) {
               keys[j] = keys[j - incr];
               values[j] = values[j - incr];
            }

            keys[j] = tempKey;
            values[j] = tempValue;
         }
      }
   }

   public static <T> int compare(T v1, T v2) {
      if (v1 == null) {
         return v2 == null ? 0 : -1;
      }

      if (v2 == null) {
         return 1;
      }

      try {
         if (v1 instanceof Comparable) {
            return ((Comparable)v1).compareTo(v2);
         }
      } catch (ClassCastException var3) {
      }

      return v1.toString().compareTo(v2.toString());
   }
}
