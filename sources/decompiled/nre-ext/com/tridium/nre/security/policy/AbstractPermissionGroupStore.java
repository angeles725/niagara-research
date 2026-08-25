package com.tridium.nre.security.policy;

import java.util.Set;

public abstract class AbstractPermissionGroupStore implements PermissionMapper {
   protected abstract void open();

   protected abstract void close();

   protected abstract void grantPermissionGroup(String var1, NiagaraPermissionGroup var2);

   protected abstract void grantPermissionGroups(String var1, Set<NiagaraPermissionGroup> var2);

   protected abstract void revokePermissionGroup(String var1, NiagaraPermissionGroup var2);

   protected abstract void revokePermissionGroups(String var1, Set<NiagaraPermissionGroup> var2);

   protected abstract Set<NiagaraPermissionGroup> getAllPermissionGroups(String var1);
}
