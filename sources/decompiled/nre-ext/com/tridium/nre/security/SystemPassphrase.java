package com.tridium.nre.security;

import com.tridium.nre.platform.IPlatformProvider;
import com.tridium.nre.platform.PlatformUtil;
import java.security.AccessController;
import java.security.Permission;
import java.util.logging.Logger;
import javax.baja.nre.util.SecurityUtil;

public class SystemPassphrase {
   public static final int MAX_SYSTEM_PASSPHRASE_LENGTH = 128;
   private static boolean SIMPLE_SP_WARNING_PRINTED = false;
   public static final String SIMPLE_SYSTEM_PASSPHRASE_NAME = ".sp";
   public static final Logger LOG = Logger.getLogger("security.systemPassphrase");

   public static void checkSystemPassphrasePermissions() {
      Permission systemPasswordPermission = new NiagaraBasicPermission("SYSTEM_PASSWORD");
      SecurityManager sm = System.getSecurityManager();

      try {
         if (sm != null) {
            sm.checkPermission(systemPasswordPermission);
         }
      } catch (NullPointerException npe) {
         throw new SecurityException("Cannot find NiagaraBasicPermission.SYSTEM_PASSWORD");
      }
   }

   public static boolean usingSimpleSystemPassphrase() {
      if (SystemPassphrase.LocalMetaDataHolder.NIAGARA_USE_SIMPLE_SP && SecurityConstants.canCheckTpk()) {
         printSimpleSystemPassphraseWarning();
      }

      return SystemPassphrase.LocalMetaDataHolder.NIAGARA_USE_SIMPLE_SP;
   }

   private static void printSimpleSystemPassphraseWarning() {
      if (!SIMPLE_SP_WARNING_PRINTED) {
         synchronized (System.err) {
            System.err.println("*************************************************************************");
            System.err.println("**** WARNING: USING SIMPLE SYSTEM PASSPHRASE, NOT FOR PRODUCTION USE ****");
            System.err.println("*************************************************************************");
         }

         SIMPLE_SP_WARNING_PRINTED = true;
      }
   }

   public static boolean checkSystemPassphrase(String candidatePassphrase) {
      checkSystemPassphrasePermissions();
      if (candidatePassphrase == null) {
         return false;
      }

      char[] decodedPasswordChars = candidatePassphrase.toCharArray();
      boolean correctCurrentPassword = false;

      try (SecretChars currentChars = SystemPassphrase.PlatformProviderHolder.PLATFORM_PROVIDER_INSTANCE.getSystemPassword()) {
         if (SecurityUtil.equals(decodedPasswordChars, currentChars.get())) {
            correctCurrentPassword = true;
         }
      }

      return correctCurrentPassword;
   }

   public static boolean usingNativeSystemPassphrase() {
      return SystemPassphrase.LocalMetaDataHolder.NIAGARA_USE_NATIVE_SYSTEM_PASSPHRASE;
   }

   private static final class LocalMetaDataHolder {
      private static final boolean NIAGARA_USE_NATIVE_SYSTEM_PASSPHRASE = AccessController.doPrivileged(
         () -> Boolean.getBoolean("niagara.use.native.system.passphrase")
      );
      private static final boolean NIAGARA_USE_SIMPLE_SP = AccessController.doPrivileged(() -> System.getenv("NIAGARA_USE_SIMPLE_SP")) != null;
   }

   private static final class PlatformProviderHolder {
      private static final IPlatformProvider PLATFORM_PROVIDER_INSTANCE = AccessController.doPrivileged(PlatformUtil::getPlatformProvider);
   }
}
