package com.tridium.fox.session;

import javax.baja.security.ReportCauseAuthenticationException;
import javax.baja.util.Lexicon;

public class FoxSecureRequiredException extends FoxAuthenticationException implements ReportCauseAuthenticationException {
   private static String SECURE_REQUIRED = "Secure Required";
   private static final String DEFAULT_CONN_TEXT = "Secure Connection Required";
   private static String message = null;

   public FoxSecureRequiredException(String method, String fatal, FoxSession session) {
      super(SECURE_REQUIRED, method, fatal, session);
   }

   public FoxSecureRequiredException(String method, FoxSession session) {
      super(SECURE_REQUIRED, method, session);
   }

   public FoxSecureRequiredException(FoxSession session) {
      super(SECURE_REQUIRED, session);
      this.session = session;
   }

   public String getCauseMessage() {
      if (message == null) {
         Lexicon lex = Lexicon.make(FoxSecureRequiredException.class);
         message = lex.get("fox.authFailed.secureRequired", "Secure Connection Required");
      }

      return message;
   }
}
