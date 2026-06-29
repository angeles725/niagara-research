package com.tridium.fox.session;

public class ServerException extends RuntimeException {
   public ServerException(String gripe) {
      super(gripe);
   }

   public ServerException(String gripe, Throwable cause) {
      super(gripe, cause);
   }
}
