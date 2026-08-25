package com.tridium.nre.util;

public class FileLockException extends RuntimeException {
   public FileLockException(Exception e) {
      super(e);
   }

   public FileLockException(String msg) {
      super(msg);
   }

   public FileLockException(String msg, Throwable cause) {
      super(msg, cause);
   }
}
