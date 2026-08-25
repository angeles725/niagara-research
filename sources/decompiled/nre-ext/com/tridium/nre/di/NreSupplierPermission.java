package com.tridium.nre.di;

import java.security.BasicPermission;
import java.security.Permission;

public final class NreSupplierPermission extends BasicPermission {
   NreSupplierPermission(Class<?> type) {
      super(type.getCanonicalName());
      this.validateName(type.getCanonicalName());
   }

   public NreSupplierPermission(String type) {
      super(type);
      this.validateName(type);
   }

   private void validateName(String type) {
      if (!"*".equals(this.getName())) {
         try {
            Class.forName(type);
         } catch (ClassNotFoundException e) {
            throw new SecurityException("invalid name provided", e);
         }
      }
   }

   @Override
   public boolean implies(Permission p) {
      if (!(p instanceof NreSupplierPermission)) {
         return false;
      } else {
         return "*".equals(this.getName()) ? true : p.getName().equals(this.getName());
      }
   }
}
