package com.tridium.niagarad.servlet;

import com.tridium.niagarad.log.ErrorHandler;
import com.tridium.niagarad.util.DaemonAuthUtil;
import com.tridium.niagarad.util.KeyedList;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import javax.baja.nre.util.TextUtil;
import javax.baja.xml.XWriter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class GetDaemonOutputServlet extends DaemonServlet {
   Map<String, String> upgradeRequestTokens = Collections.synchronizedMap(new LinkedHashMap<String, String>() {
      @Override
      protected boolean removeEldestEntry(Entry<String, String> eldest) {
         return this.size() > 10;
      }
   });
   public static final int MAX_UPGRADE_TOKENS = 10;

   public GetDaemonOutputServlet() {
      super("getdaemonoutput");
   }

   @Override
   public boolean useDefaultAuthentication() {
      return false;
   }

   @Override
   public boolean authenticate(HttpServletRequest request, HttpServletResponse response) {
      if (request.getHeader("Upgrade") != null) {
         String providedToken = request.getHeader("UpgradeRequestToken");
         if (providedToken == null) {
            return false;
         }

         String tokenKey = request.getRemoteAddr() + providedToken;
         String expectedToken = this.upgradeRequestTokens.get(tokenKey);
         if (expectedToken == null) {
            return false;
         }

         String[] tokenAttributes = TextUtil.split(expectedToken, ':');
         String expectedTokenValue = tokenAttributes[0];

         try {
            long generation = Long.valueOf(tokenAttributes[1]);
            long expiration = Long.valueOf(tokenAttributes[2]);
            long now = System.currentTimeMillis();
            if (now < generation || now > expiration) {
               this.upgradeRequestTokens.remove(tokenKey);
               return false;
            }

            if (providedToken.equals(expectedTokenValue)) {
               this.upgradeRequestTokens.remove(tokenKey);
               return true;
            }
         } catch (NumberFormatException var14) {
         }

         return false;
      } else {
         return DaemonAuthUtil.authUser(this.getServer().getAuthenticator(), request, response);
      }
   }

   @Override
   public int doGet(HttpServletRequest request, ErrorHandler handler, KeyedList query, XWriter content) {
      byte[] token = new byte[16];
      new SecureRandom().nextBytes(token);
      long tokenTimeout = request.isSecure() ? 20000L : 10000L;
      long generated = System.currentTimeMillis();
      long expires = generated + tokenTimeout;
      String tokenString = TextUtil.bytesToHexString(token);
      this.upgradeRequestTokens.put(request.getRemoteAddr() + tokenString, tokenString + ":" + generated + ":" + expires);
      content.w("<upgraderequest token='" + tokenString + "'/>");
      return 200;
   }
}
