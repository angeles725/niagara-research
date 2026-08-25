package com.tridium.nre.util.tuple;

import java.net.SocketPermission;

public class Single<T> {
   private final T t;
   private final int hash;

   public Single(T t) {
      this.t = t;
      Object firstToHash = t;
      if (t instanceof SocketPermission) {
         firstToHash = ((SocketPermission)t).getName();
      }

      this.hash = t == null ? 0 : firstToHash.hashCode();
   }

   public T getFirst() {
      return this.t;
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (o != null && this.getClass() == o.getClass()) {
         Single<?> single = (Single<?>)o;
         return this.t != null ? this.t.equals(single.t) : single.t == null;
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      return this.hash;
   }
}
