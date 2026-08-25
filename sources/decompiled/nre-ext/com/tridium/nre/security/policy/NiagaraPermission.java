package com.tridium.nre.security.policy;

import java.security.BasicPermission;
import java.security.Permission;
import java.security.PermissionCollection;
import java.util.Collections;
import java.util.Set;

public abstract class NiagaraPermission extends BasicPermission {
   protected NiagaraPermission(String name, String actions) {
      super(name, actions);
   }

   @Override
   public PermissionCollection newPermissionCollection() {
      return new SimplePermissionCollection((Class<? extends Permission>)this.getClass());
   }

   @Override
   public boolean implies(Permission permission) {
      return this.getImpliedPermissions().stream().filter(p -> p.implies(permission)).findAny().isPresent();
   }

   public Set<Class<? extends Permission>> getSupportedClasses() {
      return Collections.emptySet();
   }

   public Set<Permission> getImpliedPermissions() {
      return Collections.emptySet();
   }
}
