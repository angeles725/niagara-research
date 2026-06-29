package com.tridium.fox.session;

public class InvalidCommandException extends RuntimeException {
   public InvalidCommandException(String msg) {
      super(msg);
   }
}
