package com.tridium.nre.di;

public class NreInstantiationException extends RuntimeException {
   NreInstantiationException(String message) {
      super(message);
   }

   public NreInstantiationException(String message, Exception e) {
      super(message);
      if (e instanceof ReflectiveOperationException) {
         this.initCause(e.getCause());
      } else {
         this.initCause(e);
      }
   }
}
