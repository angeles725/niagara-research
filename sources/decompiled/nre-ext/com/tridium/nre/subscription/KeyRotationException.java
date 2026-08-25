package com.tridium.nre.subscription;

public class KeyRotationException extends Exception {
   public KeyRotationException(String msg) {
      super(msg);
   }

   public KeyRotationException(String msg, Throwable t) {
      super(msg, t);
   }
}
