package com.tridium.nre.util;

import com.tridium.nre.platform.PlatformUtil;
import java.io.File;
import java.io.IOException;
import java.security.AccessController;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.nre.util.DefaultFileCopy;

public final class NiagaraFiles {
   private static String niagaraHome = null;
   private static String niagaraUserHome = null;
   private static final String PLATFORM_BOG_FILE_NAME = "platform.bog";
   private static final String SYSTEM_PROPERTIES_FILE_NAME = "system.properties";
   private static final String LICENSE_PROPERTIES_FILE_NAME = "license.properties";
   private static final Logger sysLog = Logger.getLogger("sys.files");

   public static synchronized File getNiagaraHome() {
      if (niagaraHome == null) {
         niagaraHome = AccessController.doPrivileged(() -> System.getProperty("niagara.home"));
         if (niagaraHome == null) {
            throw new RuntimeException("'niagara.home' not defined");
         }
      }

      return new File(niagaraHome);
   }

   public static synchronized File getNiagaraUserHome() {
      if (niagaraUserHome == null) {
         niagaraUserHome = AccessController.doPrivileged(() -> System.getProperty("niagara.user.home"));
         if (niagaraUserHome == null) {
            throw new RuntimeException("'niagara.user.home' not defined");
         }
      }

      return new File(niagaraUserHome);
   }

   public static boolean isNiagaraHomeReadonly() {
      return NiagaraFiles.LocalMetaDataHolder.IS_NIAGARA_HOME_READONLY;
   }

   public static File getNiagaraHomeDefaultsPath() {
      return new File(getNiagaraHome(), "defaults");
   }

   public static File getNiagaraUserHomeEtcPath() {
      return new File(getNiagaraUserHome(), "etc");
   }

   public static File getModulesPath() {
      return isNiagaraHomeReadonly() ? new File(getNiagaraUserHome(), "modules") : getModuleDefaultsPath();
   }

   public static File getModuleDefaultsPath() {
      return new File(getNiagaraHome(), "modules");
   }

   public static File getPropertiesDefaultPath() {
      return getNiagaraHomeDefaultsPath();
   }

   public static File getPropertiesWorkingPath() {
      return isNiagaraHomeReadonly() ? getNiagaraUserHomeEtcPath() : getPropertiesDefaultPath();
   }

   public static File getPlatformBogPath() {
      return new File(getPropertiesWorkingPath(), "platform.bog");
   }

   public static File getPlatformBogDefaultsPath() {
      return new File(getPropertiesDefaultPath(), "platform.bog");
   }

   public static File getSystemPropertiesPath() {
      return new File(getPropertiesWorkingPath(), "system.properties");
   }

   public static File getSystemPropertiesDefaultPath() {
      return new File(getPropertiesDefaultPath(), "system.properties");
   }

   public static String getLicensePropertiesFileName() {
      return "license.properties";
   }

   public static File getLicensePropertiesPath() {
      return new File(getPropertiesWorkingPath(), "license.properties");
   }

   public static File getLicensePropertiesDefaultPath() {
      return new File(getPropertiesDefaultPath(), "license.properties");
   }

   public static File getWritableSecurityPath() {
      return isNiagaraHomeReadonly() ? getUserSecurityPath() : getReadOnlySecurityPath();
   }

   public static File getReadOnlySecurityPath() {
      return new File(getNiagaraHome(), "security");
   }

   public static File getUserSecurityPath() {
      return new File(getNiagaraUserHome(), "security");
   }

   public static File getPerpetualLicensePath() {
      return new File(getWritableSecurityPath(), "licenses");
   }

   public static File getSubscriptionPath() {
      return new File(getWritableSecurityPath(), "subscription");
   }

   public static File getSubscriptionLicensePath() {
      return new File(getSubscriptionPath(), "licenses");
   }

   public static File getPerpetualCertificatesPath() {
      return new File(getWritableSecurityPath(), "certificates");
   }

   public static File getSubscriptionCertificatesPath() {
      return new File(getSubscriptionPath(), "certificates");
   }

   public static File getSecuritySigningPath() {
      return new File(getUserSecurityPath(), "signing");
   }

   public static File getSecurityPolicyPath() {
      return new File(getNiagaraHome(), "bin/policy");
   }

   public static void unpackDefaults() {
      if (isNiagaraHomeReadonly()) {
         if (!getPlatformBogPath().exists()) {
            sysLog.info("Setting up platform for first use...");

            try {
               DefaultFileCopy.copyFile("platform.bog", false);
            } catch (IOException e) {
               sysLog.log(Level.WARNING, "Failed to unpack platform.bog for first use", e);
            }
         }

         if (!getSystemPropertiesPath().exists()) {
            sysLog.info("Setting up system properties for first use...");

            try {
               DefaultFileCopy.copyFile("system.properties", false);
            } catch (IOException e) {
               sysLog.log(Level.WARNING, "Failed to unpack system.properties for first use", e);
            }
         }

         if (!getLicensePropertiesPath().exists() && getLicensePropertiesDefaultPath().exists()) {
            sysLog.info("Setting up license properties for first use...");

            try {
               DefaultFileCopy.copyFile("license.properties", false);
            } catch (IOException e) {
               sysLog.log(Level.WARNING, "Failed to unpack license.properties for first use", e);
            }
         }
      }
   }

   private static final class LocalMetaDataHolder {
      public static final boolean IS_NIAGARA_HOME_READONLY = AccessController.doPrivileged(() -> PlatformUtil.getPlatformProvider().isNiagaraHomeReadonly());
   }
}
