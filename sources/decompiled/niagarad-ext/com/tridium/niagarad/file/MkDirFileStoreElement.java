package com.tridium.niagarad.file;

import java.io.File;
import java.util.logging.Logger;

public class MkDirFileStoreElement extends FileStoreElement {
   public static MkDirFileStoreElement make(String destPath, FileStore store) {
      return new MkDirFileStoreElement(destPath, store);
   }

   @Override
   public int write(byte[] buf, int len, Logger log) {
      return -1;
   }

   @Override
   public boolean commit(Logger log) {
      File dir = new File(this.destPath);
      if (!dir.exists() && !dir.mkdirs()) {
         log.severe("MkDirFileStoreElement::commit error creating dir " + this.destPath);
         this.abort(log);
         return false;
      } else {
         log.info("MkDirFileStoreElement::commit created dir " + this.destPath);
         return true;
      }
   }

   @Override
   public boolean abort(Logger log) {
      return true;
   }

   private MkDirFileStoreElement(String destPath, FileStore store) {
      super(destPath, 0L, store);
      this.size = 0L;
      this.sizeDelta = 0L;
      this.nWritten = 0L;
   }
}
