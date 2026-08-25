package com.tridium.niagarad.file;

import java.io.File;
import java.util.logging.Logger;

public class DeleteFileStoreElement extends FileStoreElement {
   boolean isDirectory;

   public static DeleteFileStoreElement make(String destPath, FileStore store) {
      File file = new File(destPath);
      if (!file.exists()) {
         return null;
      } else {
         return file.isDirectory()
            ? new DeleteFileStoreElement(destPath, (int)getDirectorySize(destPath), true, store)
            : new DeleteFileStoreElement(destPath, (int)file.length(), false, store);
      }
   }

   @Override
   public int write(byte[] buf, int len, Logger log) {
      return -1;
   }

   @Override
   public boolean commit(Logger log) {
      return this.isDirectory ? deleteDirectory(this.destPath, log) : deleteFile(this.destPath, log);
   }

   @Override
   public boolean abort(Logger log) {
      return true;
   }

   private DeleteFileStoreElement(String destPath, int size, boolean isDirectory, FileStore store) {
      super(store);
      this.destPath = destPath;
      this.size = 0L;
      this.isDirectory = isDirectory;
      this.sizeDelta = -1 * size;
   }

   private static long getDirectorySize(String dirPath) {
      long result = 0L;
      File dir = new File(dirPath);
      if (!dir.exists()) {
         return 0L;
      }

      File[] children = dir.listFiles();
      if (children != null) {
         for (File child : children) {
            if (child.isDirectory()) {
               result += getDirectorySize(child.getPath());
            } else {
               result += child.length();
            }
         }
      }

      return result;
   }

   private static boolean deleteDirectory(String dirPath, Logger log) {
      StringBuilder buffer = new StringBuilder();
      File dir = new File(dirPath);
      if (!dir.exists()) {
         return true;
      }

      File[] children = dir.listFiles();
      if (children != null) {
         for (File child : children) {
            if (child.isDirectory()) {
               if (!deleteDirectory(child.getPath(), log)) {
                  buffer.append("DeleteFileStoreElement::deleteDirectory error deleting sub directory '").append(child.getPath()).append("'");
                  log.severe(buffer.toString());
                  return false;
               }
            } else if (!deleteFile(child.getPath(), log)) {
               buffer.append("DeleteFileStoreElement::deleteDirectory error deleting sub file '").append(child.getPath()).append("'");
               log.severe(buffer.toString());
               return false;
            }
         }
      }

      if (!dir.delete()) {
         buffer.append("DeleteFileStoreElement::deleteDirectory error deleting directory '").append(dir.getPath()).append("'");
         log.severe(buffer.toString());
         return false;
      } else {
         buffer.append("DeleteFileStoreElement::deleteDirectory '").append(dir.getPath()).append("' deleted");
         log.info(buffer.toString());
         return true;
      }
   }

   private static boolean deleteFile(String filePath, Logger log) {
      File delFile = new File(filePath);
      if (delFile.delete()) {
         log.info("DeleteFileStoreElement::deleteFile " + filePath + " deleted");
         return true;
      } else {
         log.severe("DeleteFileStoreElement::deleteFile error deleting '" + filePath + "'");
         return false;
      }
   }
}
