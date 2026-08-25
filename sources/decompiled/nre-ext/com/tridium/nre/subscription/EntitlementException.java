package com.tridium.nre.subscription;

public class EntitlementException extends RuntimeException {
   public EntitlementException(String msg, Throwable cause) {
      super(msg, cause);
   }

   public EntitlementException(Throwable cause) {
      super(cause);
   }

   public EntitlementException(String msg) {
      super(msg);
   }
}
