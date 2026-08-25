package com.tridium.niagarad.servlet;

import com.tridium.niagarad.log.ErrorHandler;
import com.tridium.niagarad.log.MessageBundle;
import com.tridium.niagarad.util.CsrfTokenUtil;
import com.tridium.niagarad.util.DaemonAuthUtil;
import com.tridium.niagarad.util.KeyedList;
import com.tridium.nre.platform.IPlatformProvider;
import com.tridium.nre.security.PasswordStrength;
import com.tridium.nre.security.SecretChars;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.baja.nre.security.SharedSecretKey;
import javax.baja.nre.util.SecurityUtil;
import javax.baja.xml.XWriter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class SystemPasswordServlet extends DaemonServlet {
   private final IPlatformProvider platformProvider;

   public SystemPasswordServlet(IPlatformProvider platformProvider) {
      super("systempw");
      this.platformProvider = platformProvider;
   }

   @Override
   public boolean useDefaultAuthentication() {
      return false;
   }

   @Override
   public boolean authenticate(HttpServletRequest req, HttpServletResponse resp) {
      return DaemonAuthUtil.authAdmin(this.getServer().getAuthenticator(), req, resp);
   }

   @Override
   public int doGet(HttpServletRequest request, ErrorHandler handler, KeyedList query, XWriter content) {
      if (query != null && query.containsKey("update")) {
         if (!DebugServlet.debugEnabled && !CsrfTokenUtil.verifyCsrfToken(request, query.get("csrfToken", null))) {
            MessageBundle msg = new MessageBundle("invalid CSRF token in request");
            handler.error(msg);
            return 403;
         } else {
            return this.update(request, handler, query);
         }
      } else {
         return query != null && query.containsKey("check") ? this.check(request, handler, query) : send(content, this.platformProvider);
      }
   }

   private int check(HttpServletRequest request, ErrorHandler handler, KeyedList query) {
      if (query != null && (query.containsKey("systemPassword") || query.containsKey("oldSystemPassword"))) {
         boolean validateNewPassword = query.containsKey("systemPassword");
         String encodedSystemPasswordValue = query.get("systemPassword", null);
         String encodedOldSystemPasswordValue = query.get("oldSystemPassword", null);
         String sharedKeyAttributeName = "sharedKey_" + query.get("sharedKeyName", null);
         SharedSecretKey sharedKey = (SharedSecretKey)request.getSession(false).getAttribute(sharedKeyAttributeName);

         String decodedValue;
         try {
            decodedValue = sharedKey.decrypt(Base64.getDecoder().decode(validateNewPassword ? encodedSystemPasswordValue : encodedOldSystemPasswordValue))
               .asString(true, StandardCharsets.UTF_8);
         } catch (Exception e) {
            MessageBundle msg = new MessageBundle("SystemPw: Error decrypting system password(s) sent from client");
            handler.error(msg);
            return 400;
         }

         if (validateNewPassword) {
            if (this.platformProvider.isSystemPasswordReadonly()) {
               MessageBundle msg = new MessageBundle("platform", "SystemPwServlet.readonly", "SystemPwServlet: System passphrase is readonly");
               handler.error(msg);
               this.getServer().getFilter().severe("system passphrase is readonly");
               return 400;
            }

            if (!isPasswordValid(decodedValue, this.platformProvider)) {
               MessageBundle msg = new MessageBundle("SystemPw: Invalid password provided");
               handler.error(msg);
               return 400;
            }
         } else {
            char[] decodedPasswordChars = decodedValue.toCharArray();
            boolean correctPassword = false;
            SecretChars currentChars = this.platformProvider.getSystemPassword();
            Throwable var13 = null;

            try {
               if (SecurityUtil.equals(decodedPasswordChars, currentChars.get())) {
                  correctPassword = true;
               }
            } catch (Throwable var24) {
               var13 = var24;
               throw var24;
            } finally {
               if (currentChars != null) {
                  if (var13 != null) {
                     try {
                        currentChars.close();
                     } catch (Throwable var22) {
                        var13.addSuppressed(var22);
                     }
                  } else {
                     currentChars.close();
                  }
               }
            }

            if (!correctPassword) {
               MessageBundle msg = new MessageBundle("SystemPw: Invalid password provided");
               handler.error(msg);
               return 400;
            }
         }

         return 200;
      } else {
         return 400;
      }
   }

   private int update(HttpServletRequest request, ErrorHandler handler, KeyedList query) {
      if (this.platformProvider.isSystemPasswordReadonly()) {
         MessageBundle msg = new MessageBundle("platform", "SystemPwServlet.readonly", "SystemPwServlet: System passphrase is readonly");
         handler.error(msg);
         this.getServer().getFilter().severe("system passphrase is readonly");
         return 400;
      }

      if (query == null) {
         return 400;
      }

      String encodedSystemPassword = query.get("systemPassword", null);
      if (encodedSystemPassword == null) {
         MessageBundle msg = new MessageBundle("SystemPw: Missing system password argument");
         handler.error(msg);
         return 400;
      }

      String encodedOldSystemPassword = query.get("oldSystemPassword", null);
      if (encodedOldSystemPassword == null) {
         MessageBundle msg = new MessageBundle("SystemPw: Missing old system password argument");
         handler.error(msg);
         return 400;
      }

      String sharedKeyAttributeName = "sharedKey_" + query.get("sharedKeyName", null);
      SharedSecretKey sharedKey = (SharedSecretKey)request.getSession(false).getAttribute(sharedKeyAttributeName);

      String decodedNewSystemPasswordValue;
      String decodedOldSystemPasswordValue;
      try {
         decodedNewSystemPasswordValue = sharedKey.decrypt(Base64.getDecoder().decode(encodedSystemPassword)).asString(true, StandardCharsets.UTF_8);
         decodedOldSystemPasswordValue = sharedKey.decrypt(Base64.getDecoder().decode(encodedOldSystemPassword)).asString(true, StandardCharsets.UTF_8);
      } catch (Exception e) {
         MessageBundle msg = new MessageBundle("SystemPw: Error decrypting system password(s) sent from client");
         handler.error(msg);
         return 400;
      }

      if (!isPasswordValid(decodedNewSystemPasswordValue, this.platformProvider)) {
         MessageBundle msg = new MessageBundle("SystemPw: Invalid new password provided");
         handler.error(msg);
         return 400;
      } else if (!this.platformProvider.setSystemPassword(decodedOldSystemPasswordValue, decodedNewSystemPasswordValue)) {
         MessageBundle msg = new MessageBundle("SystemPw: Failed to set system password");
         handler.error(msg);
         return 500;
      } else {
         return 200;
      }
   }

   private static int send(XWriter content, IPlatformProvider platformProvider) {
      content.w("<systemPassword");
      char[] defaultChars = platformProvider.getDefaultPassword().toCharArray();
      SecretChars currentChars = platformProvider.getSystemPassword();
      Throwable var4 = null;

      try {
         if (SecurityUtil.equals(defaultChars, currentChars.get())) {
            content.w(' ').attr("default", "true");
         }
      } catch (Throwable var13) {
         var4 = var13;
         throw var13;
      } finally {
         if (currentChars != null) {
            if (var4 != null) {
               try {
                  currentChars.close();
               } catch (Throwable var12) {
                  var4.addSuppressed(var12);
               }
            } else {
               currentChars.close();
            }
         }
      }

      content.w(' ').attr("readonly", String.valueOf(platformProvider.isSystemPasswordReadonly()));
      content.w("/>\n");
      return 200;
   }

   private static boolean isPasswordValid(String password, IPlatformProvider platformProvider) {
      if (password.length() < 1) {
         return false;
      }

      char[] passwordChars = password.toCharArray();

      for (int i = 0; i < passwordChars.length; i++) {
         if (passwordChars[i] < '!' || passwordChars[i] > '~') {
            return false;
         }

         if (i >= 64) {
            return false;
         }
      }

      if (SecurityUtil.equals(password, platformProvider.getDefaultPassword())) {
         return false;
      }

      int lowerCase = 0;
      int upperCase = 0;
      int digits = 0;
      int special = 0;
      int len = password.length();

      for (int i = 0; i < len; i++) {
         char character = password.charAt(i);
         if (Character.isLetter(character)) {
            if (Character.isUpperCase(character)) {
               upperCase++;
            } else {
               lowerCase++;
            }
         } else if (Character.isDigit(character)) {
            digits++;
         } else {
            special++;
         }
      }

      return len >= PasswordStrength.DEFAULT.getMinimumLength()
         && len <= PasswordStrength.DEFAULT.getMaximumLength()
         && digits >= PasswordStrength.DEFAULT.getMinimumDigits()
         && lowerCase >= PasswordStrength.DEFAULT.getMinimumLowerCase()
         && upperCase >= PasswordStrength.DEFAULT.getMinimumUpperCase()
         && special >= PasswordStrength.DEFAULT.getMinimumSpecial();
   }
}
