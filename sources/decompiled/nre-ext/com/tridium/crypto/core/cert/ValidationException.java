package com.tridium.crypto.core.cert;

public class ValidationException extends Exception {
   public ValidationException(String msg) {
      super(msg);
   }

   public ValidationException(String msg, Throwable t) {
      super(msg, t);
   }
}
