package com.tridium.fox.session;

import com.tridium.fox.message.FoxMessage;

public class FoxAuthenticationException extends Exception {
   public String method = null;
   public String fatal = null;
   public FoxSession session;
   public FoxMessage data = new FoxMessage();

   public FoxAuthenticationException(String msg, String method, String fatal, FoxSession session) {
      super(msg);
      this.method = method;
      this.fatal = fatal;
      this.session = session;
   }

   public FoxAuthenticationException(String msg, String method, FoxSession session) {
      super(msg);
      this.method = method;
      this.session = session;
   }

   public FoxAuthenticationException(String msg, FoxSession session) {
      super(msg);
      this.session = session;
   }

   public FoxAuthenticationException(FoxSession session) {
      this.session = session;
   }
}
