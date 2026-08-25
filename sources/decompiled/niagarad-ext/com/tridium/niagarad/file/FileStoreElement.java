package com.tridium.niagarad.file;

import java.io.File;
import java.util.logging.Logger;

public abstract class FileStoreElement {
   public FileStoreElement next;
   public String destPath;
   public static final int RC_OK = 0;
   public static final int RC_TOO_LARGE = 1;
   public static final int RC_PERMISSION_DENIED = 2;
   public static final int RC_UNKNOWN_FILESYSTEM = 3;
   public static final int RC_UNKNOWN_ERROR = 4;
   protected int errorCode = 0;
   protected long nWritten;
   public boolean useStrictSizeChecks = true;
   protected long size;
   protected long sizeDelta;
   protected final FileStore store;

   public static FileStoreElement make(String destPath, long size, boolean isAutoCommit, FileStore store, Logger log) {
      FileStoreElement toReturn = null;
      if (isAutoCommit) {
         toReturn = new UncachedFileStoreElement(destPath, size, log, store);
      } else if (FileCachedFileStoreElement.isCacheStorageAvailable(size)) {
         toReturn = new FileCachedFileStoreElement(destPath, size, log, store);
      }

      if (toReturn != null) {
         switch (toReturn.getErrorCode()) {
            case 1:
               toReturn = InvalidFileStoreElement.TOO_LARGE;
               break;
            case 2:
               toReturn = InvalidFileStoreElement.PERMISSION_DENIED;
               break;
            case 3:
               toReturn = InvalidFileStoreElement.UNKNOWN_FILESYSTEM;
               break;
            case 4:
               toReturn = InvalidFileStoreElement.UNKNOWN_ERROR;
         }
      }

      return toReturn;
   }

   public void close() {
   }

   public abstract int write(byte[] var1, int var2, Logger var3);

   public abstract boolean commit(Logger var1);

   public abstract boolean abort(Logger var1);

   public long getSize() {
      return this.size;
   }

   public long getSizeDelta() {
      return this.sizeDelta;
   }

   protected FileStoreElement(String destPath, long size, FileStore store) {
      this.next = null;
      this.nWritten = 0L;
      this.size = size;
      this.store = store;
      int existingSize = 0;
      this.destPath = destPath;
      File file = new File(destPath);
      if (file.exists()) {
         existingSize = (int)file.length();
      }

      this.sizeDelta = size - existingSize;
   }

   public FileStore getStore() {
      return this.store;
   }

   public int getErrorCode() {
      return this.errorCode;
   }

   protected FileStoreElement(FileStore store) {
      this.next = null;
      this.destPath = null;
      this.store = store;
   }
}
