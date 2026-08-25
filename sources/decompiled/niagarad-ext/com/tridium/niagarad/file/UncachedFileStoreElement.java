package com.tridium.niagarad.file;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.nre.util.FileUtil;

public class UncachedFileStoreElement extends FileStoreElement {
   private File destinationFile;
   private OutputStream destinationFileOut;

   public UncachedFileStoreElement(String destPath, long size, Logger log, FileStore store) {
      super(destPath, size, store);
      this.destinationFile = new File(destPath);

      try {
         if (!this.destinationFile.getParentFile().exists() && !this.destinationFile.getParentFile().mkdirs()) {
            throw new IOException("Could not create parent directory");
         }

         if (!this.destinationFile.exists() && !this.destinationFile.createNewFile()) {
            throw new IOException("Could not create file");
         }

         this.destinationFileOut = new BufferedOutputStream(new FileOutputStream(this.destinationFile), FileUtil.getFileBufferSize(size));
         if (log.isLoggable(Level.FINEST)) {
            log.finest("UncachedFileStoreElement::init created file '" + destPath + "'");
         }
      } catch (IOException ioe) {
         log.severe("UncachedFileStoreElement::init failed to create file for '" + destPath + "' (" + ioe + ")");
         if (log.isLoggable(Level.FINE)) {
            log.log(Level.SEVERE, "Stack trace: ", ioe);
         }

         this.abort(log);
         if ("Permission denied".equals(ioe.getMessage())) {
            this.errorCode = 2;
         } else {
            this.errorCode = 4;
         }
      }
   }

   @Override
   public int write(byte[] buf, int len, Logger log) {
      if (this.destinationFileOut == null) {
         StringBuilder buffer = new StringBuilder();
         buffer.append("UncachedFileStoreElement::write error (").append(this.destPath).append("), file output stream undefined");
         log.severe(buffer.toString());
         this.abort(log);
         return -1;
      }

      try {
         this.destinationFileOut.write(buf, 0, len);
         return len;
      } catch (IOException ioe) {
         log.severe("UncachedFileStoreElement::write error writing file '" + this.destPath + "' (" + ioe + ")");
         if (log.isLoggable(Level.FINE)) {
            log.log(Level.SEVERE, "Stack trace: ", ioe);
         }

         this.abort(log);
         return -1;
      }
   }

   @Override
   public boolean commit(Logger log) {
      this.close();
      log.info("UncachedFileStoreElement::commit wrote '" + this.destPath + "'");
      return true;
   }

   @Override
   public boolean abort(Logger log) {
      StringBuilder buffer = new StringBuilder();
      buffer.append("UncachedFileStoreElement::abort destPath = ").append(this.destPath);
      log.fine(buffer.toString());
      this.close();
      if (this.destinationFile != null && this.destinationFile.exists() && !this.destinationFile.delete()) {
         buffer = new StringBuilder();
         buffer.append("UncachedFileStoreElement::abort failed to delete file = ").append(this.destPath);
         log.warning(buffer.toString());
      }

      return true;
   }

   @Override
   public void close() {
      if (this.destinationFileOut != null) {
         try {
            this.destinationFileOut.flush();
            this.destinationFileOut.close();
         } catch (IOException var2) {
         }

         this.destinationFileOut = null;
      }
   }
}
