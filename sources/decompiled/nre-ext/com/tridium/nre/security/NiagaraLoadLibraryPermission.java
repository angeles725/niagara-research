package com.tridium.nre.security;

import com.tridium.nre.security.policy.NiagaraPermission;
import com.tridium.nre.security.policy.NiagaraPolicyUtil;
import java.security.Permission;
import java.util.HashSet;
import java.util.Set;

public class NiagaraLoadLibraryPermission extends NiagaraPermission {
   private String regexPattern;
   private static final String LOAD_LIBRARY_PREFIX = "loadLibrary.";
   private static final Set<Class<? extends Permission>> permissionClasses = new HashSet<>();

   public NiagaraLoadLibraryPermission(String name) {
      super(name, "");
      this.regexPattern = NiagaraPolicyUtil.createRegexPatternFromName(name);
   }

   @Override
   public Set<Class<? extends Permission>> getSupportedClasses() {
      return permissionClasses;
   }

   private static boolean isNotLoadLibraryRuntimePermission(Permission permission) {
      return !(permission instanceof RuntimePermission) || !permission.getName().startsWith("loadLibrary.");
   }

   @Override
   public boolean implies(Permission permission) {
      if (isNotLoadLibraryRuntimePermission(permission)) {
         return false;
      }

      String libraryName = permission.getName().substring("loadLibrary.".length());
      return libraryName.matches(this.regexPattern);
   }

   @Override
   public String getActions() {
      return "";
   }

   static {
      permissionClasses.add(RuntimePermission.class);
   }
}
