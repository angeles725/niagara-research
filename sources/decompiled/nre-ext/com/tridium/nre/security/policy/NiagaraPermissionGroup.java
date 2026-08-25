package com.tridium.nre.security.policy;

import java.security.Permission;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public abstract class NiagaraPermissionGroup {
   private final NiagaraPermissionGroup.RiskLevel riskLevel;
   private final EnumSet<NiagaraPermissionGroup.Flags> flags;
   private static Map<String, NiagaraPermissionGroup> permissionGroupTypeMap = null;

   protected NiagaraPermissionGroup(NiagaraPermissionGroup.RiskLevel riskLevel, EnumSet<NiagaraPermissionGroup.Flags> flags) {
      this.riskLevel = riskLevel;
      this.flags = flags;
   }

   public boolean requiresSignature() {
      return false;
   }

   public abstract String getType();

   public abstract String getPurpose();

   public abstract String getParameters();

   public NiagaraPermissionGroup.RiskLevel getRiskLevel() {
      return this.riskLevel;
   }

   public abstract String getLocalizedRiskLevel();

   public abstract String getRiskDescription();

   protected EnumSet<NiagaraPermissionGroup.Flags> getFlags() {
      return this.flags;
   }

   public boolean hasFlag(NiagaraPermissionGroup.Flags desiredFlag) {
      return this.flags.contains(desiredFlag);
   }

   public abstract Set<Permission> getImpliedPermissions();

   protected abstract NiagaraPermissionGroup copy();

   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (o != null && this.getClass() == o.getClass()) {
         NiagaraPermissionGroup that = (NiagaraPermissionGroup)o;
         return this.getImpliedPermissions().equals(that.getImpliedPermissions());
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      return this.getImpliedPermissions().isEmpty() ? this.getImpliedPermissions().hashCode() : 0;
   }

   protected static void initPermissionGroupMap(Map<String, NiagaraPermissionGroup> map) {
      if (permissionGroupTypeMap == null) {
         permissionGroupTypeMap = new HashMap<>();
         permissionGroupTypeMap.putAll(map);
      }
   }

   public static NiagaraPermissionGroup getPermissionGroupForType(String type) {
      return permissionGroupTypeMap != null ? permissionGroupTypeMap.get(type) : null;
   }

   public static Set<String> getPermissionGroupTypes() {
      return permissionGroupTypeMap.keySet();
   }

   public enum Flags {
      REQUIRED;
   }

   public enum RiskLevel {
      MILD,
      MODERATE,
      SEVERE;
   }
}
