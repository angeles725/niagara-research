package com.tridium.niagarad.file;

import com.tridium.niagarad.NiagaraDaemon;
import java.io.FileInputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

public class KeyRingImportFileStoreElement extends FileCachedFileStoreElement {
   public KeyRingImportFileStoreElement(String destPath, long size, Logger log, FileStore store) {
      super(destPath, size, log, store);
   }

   @Override
   public boolean commit(Logger log) {
      StringBuilder buffer = new StringBuilder();

      try (FileInputStream in = new FileInputStream(this.tempFile)) {
         NiagaraDaemon.getSecurityInfoProvider().getKeyRing().importKeyData(in, (int)this.nWritten, this.getStore().getPBEKey());
         log.info("KeyRingImportFileStoreElement::commit imported data to keyring in " + this.destPath);
      } catch (Exception e) {
         buffer.append("KeyRingImportFileStoreElement::commit error '").append(this.destPath).append("' (").append(e).append(")");
         log.severe(buffer.toString());
         if (log.isLoggable(Level.FINE)) {
            log.log(Level.SEVERE, "Stack trace: ", e);
         }

         this.abort(log);
         return false;
      }

      if (this.tempFile.exists() && !this.tempFile.delete()) {
         buffer = new StringBuilder();
         buffer.append("KeyRingImportFileStoreElement::commit error deleting temp file '").append(this.tempFileName).append("'");
         log.warning(buffer.toString());
      }

      return true;
   }
}
