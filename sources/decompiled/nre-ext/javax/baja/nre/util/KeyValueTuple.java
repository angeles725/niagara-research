package javax.baja.nre.util;

import java.util.Objects;

public class KeyValueTuple<K, V> {
   public final K key;
   public final V value;

   public KeyValueTuple(K key, V value) {
      Objects.requireNonNull(key);
      Objects.requireNonNull(value);
      this.key = key;
      this.value = value;
   }

   @Override
   public boolean equals(Object v) {
      return v != null
         && v instanceof KeyValueTuple
         && Objects.equals(((KeyValueTuple)v).key, this.key)
         && Objects.equals(((KeyValueTuple)v).value, this.value);
   }

   @Override
   public int hashCode() {
      return Objects.hash(this.key, this.value);
   }
}
