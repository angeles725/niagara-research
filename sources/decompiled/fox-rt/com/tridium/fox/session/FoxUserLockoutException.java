package com.tridium.fox.session;

import javax.baja.security.ReportCauseAuthenticationException;
import javax.baja.util.Lexicon;

public class FoxUserLockoutException extends FoxAuthenticationException implements ReportCauseAuthenticationException {
   private static String LOCKOUT = "Lockout";
   private static final String DEFAULT_LOCKOUT_TEXT = "User Lockout";
   private static String message = null;

   public FoxUserLockoutException(String method, String fatal, FoxSession session) {
      super(LOCKOUT, method, fatal, session);
   }

   public FoxUserLockoutException(String method, FoxSession session) {
      super(LOCKOUT, method, session);
   }

   public FoxUserLockoutException(FoxSession session) {
      super(LOCKOUT, session);
      this.session = session;
   }

   public String getCauseMessage() {
      if (message == null) {
         Lexicon lex = Lexicon.make(FoxUserLockoutException.class);
         message = lex.get("fox.authFailed.userLockout", "User Lockout");
      }

      return message;
   }
}
