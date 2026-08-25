package com.tridium.niagarad.security.policy;

import com.tridium.nre.security.policy.AbstractPermissionGroupStore;
import com.tridium.nre.security.policy.NiagaraPermissionGroup;
import java.security.Permission;
import java.util.Set;

public class NoOpPermissionGroupStore extends AbstractPermissionGroupStore {
   protected void open() {
   }

   protected void close() {
   }

   protected void grantPermissionGroup(String moduleUrl, NiagaraPermissionGroup permissionGroup) {
      throw new UnsupportedOperationException();
   }

   protected void grantPermissionGroups(String moduleUrl, Set<NiagaraPermissionGroup> permissionGroups) {
      throw new UnsupportedOperationException();
   }

   protected void revokePermissionGroup(String moduleUrl, NiagaraPermissionGroup permissionGroup) {
      throw new UnsupportedOperationException();
   }

   protected void revokePermissionGroups(String moduleUrl, Set<NiagaraPermissionGroup> permissionGroups) {
      throw new UnsupportedOperationException();
   }

   protected Set<NiagaraPermissionGroup> getAllPermissionGroups(String moduleUrl) {
      return null;
   }

   public boolean appliesTo(String url) {
      return false;
   }

   public Set<Permission> get(String url) {
      return null;
   }
}
