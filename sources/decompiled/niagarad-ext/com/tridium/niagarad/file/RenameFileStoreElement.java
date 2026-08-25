package com.tridium.niagarad.file;

import java.io.File;
import java.util.logging.Logger;

public class RenameFileStoreElement extends FileStoreElement {
   private final String srcPath;

   public static RenameFileStoreElement make(String srcPath, String destPath, FileStore store) {
      return new RenameFileStoreElement(srcPath, destPath, store);
   }

   @Override
   public int write(byte[] buf, int len, Logger log) {
      return -1;
   }

   @Override
   public boolean commit(Logger log) {
      File srcFile = new File(this.srcPath);
      if (!srcFile.exists()) {
         log.severe("RenameFileStoreElement::commit error, cannot find file " + this.srcPath);
         this.abort(log);
         return false;
      } else {
         File destFile = new File(this.destPath);
         if (destFile.exists()) {
            log.severe("RenameFileStoreElement::commit error, file " + this.destPath + " exists");
            this.abort(log);
            return false;
         } else if (!srcFile.renameTo(destFile)) {
            log.severe("RenameFileStoreElement::commit error " + this.srcPath + "->" + this.destPath + " failed");
            this.abort(log);
            return false;
         } else {
            log.info("RenameFileStoreElement::commit renamed " + this.srcPath + "->" + this.destPath);
            return true;
         }
      }
   }

   @Override
   public boolean abort(Logger log) {
      return true;
   }

   private RenameFileStoreElement(String srcPath, String destPath, FileStore store) {
      super(destPath, 0L, store);
      this.srcPath = srcPath;
      this.size = 0L;
      this.sizeDelta = 0L;
      this.nWritten = 0L;
   }
}
