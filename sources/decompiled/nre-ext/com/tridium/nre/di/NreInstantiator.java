package com.tridium.nre.di;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class NreInstantiator {
   private Map<Class<?>, TypeSupplier<?>> suppliers = new ConcurrentHashMap<>();

   public void addSupplier(TypeSupplier<?> supplier) {
      SecurityManager sm = System.getSecurityManager();
      if (sm != null) {
         NreSupplierPermission supplierPermission = new NreSupplierPermission(supplier.getType());
         sm.checkPermission(supplierPermission);
      }

      if (this.suppliers.containsKey(supplier.getType())) {
         throw new IllegalStateException(String.format("supplier for %s already exists", supplier.getType()));
      }

      this.suppliers.put(supplier.getType(), supplier);
   }

   public TypeSupplier<?> getSupplier(Class<?> type) {
      SecurityManager sm = System.getSecurityManager();
      if (sm != null) {
         NreSupplierPermission supplierPermission = new NreSupplierPermission(type);
         sm.checkPermission(supplierPermission);
      }

      return this.suppliers.containsKey(type) ? this.suppliers.get(type) : null;
   }

   public void removeSupplier(TypeSupplier<?> supplier) {
      this.removeSupplier(supplier.getType());
   }

   public void removeSupplier(Class<?> type) {
      SecurityManager sm = System.getSecurityManager();
      if (sm != null) {
         NreSupplierPermission supplierPermission = new NreSupplierPermission(type);
         sm.checkPermission(supplierPermission);
      }

      if (this.suppliers.containsKey(type)) {
         this.suppliers.remove(type);
      }
   }

   public <T> T instance(Class<T> type) {
      try {
         TypeSupplier<?> supplier = this.suppliers.get(type);
         if (supplier == null) {
            throw new IllegalStateException(String.format("supplier for %s not found", type.getName()));
         } else if (type.isAssignableFrom(supplier.getType())) {
            return (T)supplier.get(this);
         } else {
            throw new IllegalStateException(String.format("invalid supplier implementation: %s", supplier.getClass().getName()));
         }
      } catch (IllegalStateException | NreInstantiationException e) {
         throw e;
      } catch (Exception e) {
         throw new NreInstantiationException("unable to return instance", e);
      }
   }
}
