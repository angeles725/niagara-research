package com.tridium.niagarad.file;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.nre.util.FileUtil;
import javax.baja.nre.util.TextUtil;

public class FileCachedFileStoreElement extends FileStoreElement {
   protected String tempFileName = null;
   protected File tempFile = null;
   protected OutputStream tempFileOut = null;
   protected boolean sameFileSystem = false;
   private static final int MAX_TEMPORARY_FILENAME = 64;

   public FileCachedFileStoreElement(String destPath, long size, Logger log, FileStore store) {
      super(destPath, size, store);

      try {
         String temporaryFilePrefix = TextUtil.truncate("fcfse-" + new File(destPath).getName(), 64);
         this.tempFile = File.createTempFile(temporaryFilePrefix, "", new File(FileStore.getTempDirPath()));
         this.tempFileName = this.tempFile.getPath();
         this.tempFileOut = new BufferedOutputStream(new FileOutputStream(this.tempFile), FileUtil.getFileBufferSize(size));
         if (log.isLoggable(Level.FINEST)) {
            log.finest("FileCachedFileStoreElement::init created temp file '" + this.tempFileName + "'");
         }
      } catch (IOException ioe) {
         log.severe("FileCachedFileStoreElement::init failed to create temporary file for '" + destPath + "' (" + ioe + ")");
         if (log.isLoggable(Level.FINE)) {
            log.log(Level.SEVERE, "Temporary location: '" + new File(FileStore.getTempDirPath(), "fcfse-" + new File(destPath).getName() + "'"));
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
      if (this.tempFileOut == null) {
         StringBuilder buffer = new StringBuilder();
         buffer.append("FileCachedFileStoreElement::write error (").append(this.destPath).append("), temp file output stream undefined");
         log.severe(buffer.toString());
         this.abort(log);
         return -1;
      }

      if (this.useStrictSizeChecks && this.nWritten + len > this.size) {
         StringBuilder buffer = new StringBuilder();
         buffer.append("FileCachedFileStoreElement::write error (").append(this.destPath).append("), buffer overrun");
         log.severe(buffer.toString());
         this.abort(log);
         return -1;
      }

      try {
         this.tempFileOut.write(buf, 0, len);
      } catch (IOException ioe) {
         StringBuilder buffer = new StringBuilder();
         buffer.append("FileCachedFileStoreElement::write error (").append(this.destPath).append("), temp file write error (").append(ioe).append(")");
         log.severe(buffer.toString());
         if (log.isLoggable(Level.FINE)) {
            log.log(Level.SEVERE, "Stack trace: ", ioe);
         }

         this.abort(log);
         return -1;
      }

      this.nWritten += len;
      return len;
   }

   @Override
   public boolean commit(Logger log) {
      if (this.useStrictSizeChecks && this.nWritten != this.size) {
         StringBuilder buffer = new StringBuilder();
         buffer.append("FileCachedFileStoreElement::commit error committing file transfer: file size mismatch");
         log.severe(buffer.toString());
         this.abort(log);
         return false;
      }

      File destinationFile = new File(this.destPath);
      if (!destinationFile.getParentFile().exists() && !destinationFile.getParentFile().mkdirs()) {
         StringBuilder buffer = new StringBuilder();
         buffer.append("FileCachedFileStoreElement::commit error committing '")
            .append(this.tempFileName)
            .append("' to '")
            .append(this.destPath)
            .append("' failed to create parent directory ('")
            .append(destinationFile.getParentFile().getPath())
            .append("')");
         log.severe(buffer.toString());
         this.abort(log);
         return false;
      }

      boolean previouslyExisted = destinationFile.exists();
      boolean moveFailed = false;
      if (this.sameFileSystem) {
         try {
            FileUtil.move(this.tempFile, destinationFile, true);
         } catch (IOException moveIOE) {
            moveFailed = true;
         }
      }

      if (moveFailed || !this.sameFileSystem) {
         try {
            FileUtil.copyFile(this.tempFile, destinationFile);
            if (this.tempFile.exists() && !this.tempFile.delete()) {
               StringBuilder buffer = new StringBuilder();
               buffer.append("FileCachedFileStoreElement::commit error deleting temp file '").append(this.tempFileName).append("'");
               log.warning(buffer.toString());
            }
         } catch (IOException copyIOE) {
            StringBuilder buffer = new StringBuilder();
            buffer.append("FileCachedFileStoreElement::commit error copying '")
               .append(this.tempFileName)
               .append("' to '")
               .append(this.destPath)
               .append("' (")
               .append(copyIOE)
               .append(")");
            log.severe(buffer.toString());
            if (log.isLoggable(Level.FINE)) {
               log.log(Level.SEVERE, "Stack trace: ", copyIOE);
            }

            if (!previouslyExisted && destinationFile.exists() && !destinationFile.delete()) {
               buffer = new StringBuilder();
               buffer.append("FileCachedFileStoreElement::commit error deleting partial file '").append(destinationFile).append("'");
               log.warning(buffer.toString());
            }

            return false;
         }
      }

      StringBuilder buffer = new StringBuilder();
      buffer.append("FileCachedFileStoreElement::commit file written ").append(this.destPath);
      log.info(buffer.toString());
      return true;
   }

   @Override
   public boolean abort(Logger log) {
      StringBuilder buffer = new StringBuilder();
      buffer.append("FileCachedFileStoreElement::abort destPath = ").append(this.destPath);
      log.fine(buffer.toString());
      this.close();
      if (this.tempFile != null && this.tempFile.exists() && !this.tempFile.delete()) {
         buffer = new StringBuilder();
         buffer.append("FileCachedFileStoreElement::abort failed to delete temp file = ").append(this.tempFile.getPath());
         log.warning(buffer.toString());
      }

      return true;
   }

   @Override
   public void close() {
      if (this.tempFileOut != null) {
         try {
            this.tempFileOut.flush();
            this.tempFileOut.close();
         } catch (IOException var2) {
         }

         this.tempFileOut = null;
      }
   }

   public static boolean isCacheStorageAvailable(long size) {
      return size < FileStore.getFreeSpace(FileStore.getTempDirPath());
   }
}
