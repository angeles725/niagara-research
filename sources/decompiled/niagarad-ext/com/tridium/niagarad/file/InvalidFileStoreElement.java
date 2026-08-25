package com.tridium.niagarad.file;

import java.util.logging.Logger;

public class InvalidFileStoreElement extends FileStoreElement {
   public static final InvalidFileStoreElement TOO_LARGE = new InvalidFileStoreElement(1);
   public static final InvalidFileStoreElement PERMISSION_DENIED = new InvalidFileStoreElement(2);
   public static final InvalidFileStoreElement UNKNOWN_FILESYSTEM = new InvalidFileStoreElement(3);
   public static final InvalidFileStoreElement UNKNOWN_ERROR = new InvalidFileStoreElement(4);

   private InvalidFileStoreElement(int errorCode) {
      super(null);
      this.errorCode = errorCode;
   }

   @Override
   public int write(byte[] buf, int len, Logger log) {
      return 0;
   }

   @Override
   public boolean commit(Logger log) {
      return false;
   }

   @Override
   public boolean abort(Logger log) {
      return false;
   }
}
