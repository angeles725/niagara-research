package com.tridium.lonworks;

public class CancelOperationException extends RuntimeException {
   public CancelOperationException() {
      super("User canceled operation");
   }
}
