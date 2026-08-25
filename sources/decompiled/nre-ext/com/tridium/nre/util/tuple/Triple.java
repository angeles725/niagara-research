package com.tridium.nre.util.tuple;

import java.net.SocketPermission;
import java.util.Objects;

public class Triple<T, U, V> {
   private final T t;
   private final U u;
   private final V v;
   private final int hash;

   public Triple(T t, U u, V v) {
      this.t = t;
      this.u = u;
      this.v = v;
      Object firstToHash = t;
      Object secondToHash = u;
      Object thirdToHash = v;
      if (t instanceof SocketPermission) {
         firstToHash = ((SocketPermission)t).getName();
      }

      if (u instanceof SocketPermission) {
         secondToHash = ((SocketPermission)u).getName();
      }

      if (v instanceof SocketPermission) {
         thirdToHash = ((SocketPermission)v).getName();
      }

      this.hash = Objects.hash(firstToHash, secondToHash, thirdToHash);
   }

   public T getFirst() {
      return this.t;
   }

   public U getSecond() {
      return this.u;
   }

   public V getThird() {
      return this.v;
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      }

      if (o != null && this.getClass() == o.getClass()) {
         Triple<?, ?, ?> triple = (Triple<?, ?, ?>)o;
         if (this.t != null ? this.t.equals(triple.t) : triple.t == null) {
            if (this.u != null ? this.u.equals(triple.u) : triple.u == null) {
               return this.v != null ? this.v.equals(triple.v) : triple.v == null;
            } else {
               return false;
            }
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      return this.hash;
   }
}
