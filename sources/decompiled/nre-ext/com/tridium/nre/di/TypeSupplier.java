package com.tridium.nre.di;

public interface TypeSupplier<T> {
   T get(NreInstantiator var1);

   Class<T> getType();
}
