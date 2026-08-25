package com.tridium.niagarad.util;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Objects;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

public class CsrfTokenUtil {
   public static final int CSRF_TOKEN_BIT_LEN = 192;
   public static final String CSRF_TOKEN_KEY = "csrfToken";
   private static final SecureRandom rand = new SecureRandom();

   public static boolean csrfTokenExists(HttpServletRequest req) {
      HttpSession session = req.getSession(false);
      return session != null && session.getAttribute("csrfToken") != null;
   }

   public static String getCsrfToken(HttpServletRequest req) {
      HttpSession session = req.getSession(false);
      if (session == null) {
         return null;
      }

      synchronized (session) {
         String csrfToken = getCsrfTokenFromSession(session);
         if (csrfToken == null) {
            byte[] bytes = new byte[24];
            rand.nextBytes(bytes);
            csrfToken = Base64.getEncoder().encodeToString(bytes);
            session.setAttribute("csrfToken", csrfToken);
         }

         return csrfToken;
      }
   }

   public static boolean verifyCsrfToken(HttpSession session, String srcCsrfToken) {
      if (Objects.isNull(srcCsrfToken)) {
         return false;
      }

      String csrfToken = getCsrfTokenFromSession(session);
      return Objects.isNull(csrfToken) ? false : csrfToken.equals(srcCsrfToken);
   }

   public static boolean verifyCsrfToken(HttpServletRequest req, String srcCsrfToken) {
      if (Objects.isNull(srcCsrfToken)) {
         return false;
      }

      String csrfToken = getCsrfTokenFromSession(req.getSession(false));
      return Objects.isNull(csrfToken) ? false : csrfToken.equals(srcCsrfToken);
   }

   private static String getCsrfTokenFromSession(HttpSession session) {
      if (session == null) {
         return null;
      }

      try {
         String csrfToken = (String)session.getAttribute("csrfToken");
         return csrfToken == null ? null : csrfToken;
      } catch (Exception e) {
         return null;
      }
   }
}
