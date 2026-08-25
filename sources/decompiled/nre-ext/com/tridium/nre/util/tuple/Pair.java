package com.tridium.nre.util.tuple;

import java.net.SocketPermission;
import java.util.Objects;

public class Pair<T, U> {
   private final T t;
   private final U u;
   private final int hash;

   public Pair(T t, U u) {
      this.t = t;
      this.u = u;
      Object firstToHash = t;
      Object secondToHash = u;
      if (t instanceof SocketPermission) {
         firstToHash = ((SocketPermission)t).getName();
      }

      if (u instanceof SocketPermission) {
         secondToHash = ((SocketPermission)u).getName();
      }

      this.hash = Objects.hash(firstToHash, secondToHash);
   }

   public T getFirst() {
      return this.t;
   }

   public U getSecond() {
      return this.u;
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      }

      if (o != null && this.getClass() == o.getClass()) {
         Pair<?, ?> pair = (Pair<?, ?>)o;
         if (this.t != null ? this.t.equals(pair.t) : pair.t == null) {
            return this.u != null ? this.u.equals(pair.u) : pair.u == null;
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
