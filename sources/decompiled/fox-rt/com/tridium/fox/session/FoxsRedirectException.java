package com.tridium.fox.session;

public class FoxsRedirectException extends Exception {
   private int port = 4911;

   public FoxsRedirectException(int port) {
      this.port = port;
   }

   public int getPort() {
      return this.port;
   }
}
