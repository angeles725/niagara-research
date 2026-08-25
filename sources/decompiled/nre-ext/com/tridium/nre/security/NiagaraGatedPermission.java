package com.tridium.nre.security;

import com.tridium.nre.security.policy.NiagaraPermission;
import java.security.Permission;

public abstract class NiagaraGatedPermission extends NiagaraPermission {
   protected Permission basePermission;

   protected NiagaraGatedPermission(Permission basePermission, String name, String actions) {
      super(name, actions);
      if (basePermission instanceof NiagaraGatedPermission) {
         throw new IllegalArgumentException("A NiagaraGatedPermission cannot be a basePermission to another NiagaraGatedPermission");
      }

      this.basePermission = basePermission;
   }

   @Override
   public final boolean implies(Permission p) {
      return p instanceof NiagaraGatedPermission
         ? this.impliesGatedPermission((NiagaraGatedPermission)p)
         : this.basePermission.implies(p) && this.satisfiesCondition();
   }

   protected abstract boolean satisfiesCondition();

   protected abstract boolean impliesGatedPermission(NiagaraGatedPermission var1);
}
