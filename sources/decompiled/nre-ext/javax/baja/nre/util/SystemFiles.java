package javax.baja.nre.util;

import com.tridium.nre.util.NiagaraFiles;
import java.io.File;

public final class SystemFiles {
   public static File getPlatformBog() {
      return NiagaraFiles.getPlatformBogPath();
   }

   public static File getSystemProperties() {
      return NiagaraFiles.getSystemPropertiesPath();
   }

   public static File getLicenseProperties() {
      return NiagaraFiles.getLicensePropertiesPath();
   }

   public static File getNiagaraHomeDirectory() {
      return NiagaraFiles.getNiagaraHome();
   }

   public static boolean isNiagaraHomeReadOnly() {
      return NiagaraFiles.isNiagaraHomeReadonly();
   }

   public static File getModulesDirectory() {
      return NiagaraFiles.getModulesPath();
   }

   public static File getPerpetualCertificatesDirectory() {
      return NiagaraFiles.getPerpetualCertificatesPath();
   }

   public static File getPerpetualLicensesDirectory() {
      return NiagaraFiles.getPerpetualLicensePath();
   }

   public static File getSubscriptionCertificatesDirectory() {
      return NiagaraFiles.getSubscriptionCertificatesPath();
   }

   public static File getSubscriptionLicensesDirectory() {
      return NiagaraFiles.getSubscriptionLicensePath();
   }

   private SystemFiles() {
   }
}
