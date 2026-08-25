package javax.baja.nre.util;

import com.tridium.nre.util.NiagaraFiles;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DefaultFileCopy {
   private static final Logger sysLog = Logger.getLogger("sys.files");

   public static void copyFile(String fileName) throws IllegalStateException, IOException {
      copyFile(fileName, false);
   }

   public static void copyFile(String fileName, boolean overWrite) throws IllegalStateException, IOException {
      try {
         File src = SecurityUtil.resolveChrootPath(NiagaraFiles.getNiagaraHomeDefaultsPath(), fileName);
         File target = SecurityUtil.resolveChrootPath(NiagaraFiles.getNiagaraUserHomeEtcPath(), fileName);
         if (target.exists()) {
            if (!overWrite) {
               return;
            }

            FileUtil.delete(target);
         }

         if (sysLog.isLoggable(Level.FINE)) {
            sysLog.fine("copying default file for '" + target + "'");
         }

         File parent = target.getParentFile();
         if (!parent.exists() && !parent.mkdirs()) {
            sysLog.warning("failed to create directory '" + parent + "', may be unable to copy default file for '" + fileName + "'");
         }

         FileUtil.copyFile(src, target);
      } catch (Exception e) {
         sysLog.log(Level.SEVERE, "failed to copy default file '" + fileName + "'", e);
         throw e;
      }
   }

   public static void copyDir(String dirName, boolean overWrite) throws IllegalStateException, IOException {
      try {
         File src = SecurityUtil.resolveChrootPath(NiagaraFiles.getNiagaraHomeDefaultsPath(), dirName);
         File target = SecurityUtil.resolveChrootPath(NiagaraFiles.getNiagaraUserHomeEtcPath(), dirName);
         if (target.exists()) {
            if (!overWrite) {
               return;
            }

            FileUtil.delete(target);
         }

         if (sysLog.isLoggable(Level.FINE)) {
            sysLog.fine("copying default file for '" + target + "'");
         }

         FileUtil.copyDir(src, target);
      } catch (Exception e) {
         sysLog.log(Level.SEVERE, "failed to copy default directory '" + dirName + "'", e);
      }
   }
}
