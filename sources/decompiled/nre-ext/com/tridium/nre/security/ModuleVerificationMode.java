package com.tridium.nre.security;

import com.tridium.nre.util.Version;

public enum ModuleVerificationMode {
   noPreference,
   low,
   medium,
   high;

   public static final ModuleVerificationMode DEFAULT = medium;
   private static final Version MIN_VERIFICATION_VERSION = new Version("4.8");

   public static ModuleVerificationMode make(ModuleVerificationMode preferredVerificationMode, Version version) {
      if (version.compareTo(MIN_VERIFICATION_VERSION) < 0) {
         return low;
      } else {
         return preferredVerificationMode != noPreference && preferredVerificationMode != null
            ? preferredVerificationMode
            : getDefaultVerificationMode(version);
      }
   }

   public static ModuleVerificationMode max(ModuleVerificationMode verificationMode1, ModuleVerificationMode verificationMode2) {
      return verificationMode1.compareTo(verificationMode2) > 0 ? verificationMode1 : verificationMode2;
   }

   private static ModuleVerificationMode getDefaultVerificationMode(Version version) {
      if (version.compareTo(new Version("4.9")) < 0) {
         return low;
      } else {
         return version.compareTo(new Version("4.10")) < 0 ? medium : DEFAULT;
      }
   }
}
