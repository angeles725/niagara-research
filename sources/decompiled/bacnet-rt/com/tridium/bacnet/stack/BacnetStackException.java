package com.tridium.bacnet.stack;

import javax.baja.bacnet.BacnetException;

public class BacnetStackException extends BacnetException implements BacnetStackErrorCodes {
   public BacnetStackException(String msg, Throwable cause) {
      super(msg, cause);
   }

   public BacnetStackException(Throwable cause) {
      super(cause);
   }

   public BacnetStackException(String msg) {
      super(msg);
   }

   public BacnetStackException() {
   }

   @Override
   public String toString() {
      return "Stack:" + this.getMessage();
   }
}
