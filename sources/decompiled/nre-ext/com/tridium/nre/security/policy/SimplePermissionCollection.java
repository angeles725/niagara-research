package com.tridium.nre.security.policy;

import java.security.Permission;
import java.security.PermissionCollection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

public class SimplePermissionCollection extends PermissionCollection {
   private final Class<? extends Permission> cls;
   private final Set<Permission> permissions = new HashSet<>();

   public SimplePermissionCollection(Class<? extends Permission> cls) {
      this.cls = cls;
   }

   @Override
   public void add(Permission permission) {
      if (!permission.getClass().equals(this.cls)) {
         throw new IllegalArgumentException(String.format("Class %s is not %s", permission.getClass().toString(), this.cls.toString()));
      }

      if (this.isReadOnly()) {
         throw new SecurityException("Attempt to modify a read-only PermissionCollection");
      }

      synchronized (this) {
         this.permissions.add(permission);
      }
   }

   @Override
   public synchronized boolean implies(Permission permission) {
      return this.permissions.stream().filter(p -> p.implies(permission)).findAny().isPresent();
   }

   @Override
   public synchronized Enumeration<Permission> elements() {
      return Collections.enumeration(this.permissions);
   }
}
