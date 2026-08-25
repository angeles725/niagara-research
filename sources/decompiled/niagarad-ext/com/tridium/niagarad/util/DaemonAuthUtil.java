package com.tridium.niagarad.util;

import com.tridium.niagarad.NiagaraDaemon;
import com.tridium.niagarad.http.Authenticator;
import com.tridium.niagarad.security.AuthenticationInfo;
import com.tridium.niagarad.security.SimpleAuthenticationInfo;
import com.tridium.nre.platform.IPlatformProvider;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Properties;
import java.util.logging.Level;
import javax.baja.nre.util.TextUtil;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public final class DaemonAuthUtil {
   public static final String _BASIC_NATIVE = "basic/native";
   public static final String _DIGEST_FILE = "digest/file";
   public static final String _SCRAM_GLIBC_SHA512_FILE = "scram-glibc-sha512/file";
   public static final String _SCRAMSHA512_NATIVE = "scram-sha512/native";
   public static final String _SCRAM_BCRYPT_NATIVE = "scram-bcrypt/native";
   public static final String _SCRAM_GLIBC_SHA256_NATIVE = "scram-glibc-sha256/native";
   public static final String _SCRAM_GLIBC_SHA512_NATIVE = "scram-glibc-sha512/native";
   public static String[] knownAuthenticationTypes = new String[]{
      "basic/native", "scram-bcrypt/native", "scram-glibc-sha256/native", "scram-glibc-sha512/native", "scram-sha512/native", "scram-glibc-sha512/file"
   };
   public static final int _DEFAULT_GLIBC_SHA_ITERATIONS = 5000;
   public static final int _DEFAULT_QNX_SHA_ITERATIONS = 4096;
   public static final int _PREFERRED_HASH_CREATE_ITERATIONS = 10000;
   public static final String HTTP_REQUEST_ATTR_AUTHENTICATION_INFO = "AuthenticationInfo";
   public static final String HTTP_RESPONSE_HEADER_STATION_NOT_ADMIN = "StationNotAdmin";
   public static final String LEGACY_DEFAULT_USERNAME = "tridium";
   public static final String LEGACY_DEFAULT_PASSWORD = "niagara";

   private DaemonAuthUtil() {
   }

   public static String getDefaultAuthScheme(IPlatformProvider platformProvider) {
      String supportedAuthenticationTypes = platformProvider.getSupportedAuthenticationTypes();
      if (supportedAuthenticationTypes.contains("scram-sha512/native")) {
         return "scram-sha512/native";
      } else if (supportedAuthenticationTypes.contains("scram-glibc-sha512/native")) {
         return "scram-glibc-sha512/native";
      } else if (supportedAuthenticationTypes.contains("scram-glibc-sha256/native")) {
         return "scram-glibc-sha256/native";
      } else if (supportedAuthenticationTypes.contains("scram-bcrypt/native")) {
         return "scram-bcrypt/native";
      } else if (supportedAuthenticationTypes.contains("basic/native")) {
         return "basic/native";
      } else if (supportedAuthenticationTypes.contains("scram-glibc-sha512/file")) {
         return "scram-glibc-sha512/file";
      } else {
         throw new IllegalStateException("No supported daemon authentication types recognized (" + supportedAuthenticationTypes + ")");
      }
   }

   public static boolean authAdmin(Authenticator authenticator, HttpServletRequest req, HttpServletResponse resp) {
      if (authenticator != null && authenticator.getLog().isLoggable(Level.FINEST)) {
         authenticator.getLog().finest("authAdmin authentication utility method used for request '" + TextUtil.truncate(req.getRequestURI(), 25) + "'");
      }

      boolean authInfoIsAdmin;
      if (!authUser(authenticator, req, resp)) {
         authInfoIsAdmin = false;
      } else {
         AuthenticationInfo authInfo = (AuthenticationInfo)req.getAttribute("AuthenticationInfo");
         if (!(authInfo instanceof SimpleAuthenticationInfo)) {
            if (authenticator != null && authenticator.getLog().isLoggable(Level.FINEST)) {
               authenticator.getLog()
                  .finest(
                     "authAdmin can not authenticate request '"
                        + TextUtil.truncate(req.getRequestURI(), 25)
                        + "' with invalid "
                        + "AuthenticationInfo"
                        + " attribute value '"
                        + (authInfo == null ? "" : TextUtil.getClassName(authInfo.getClass()))
                        + "'"
                  );
            }

            authInfoIsAdmin = false;
         } else {
            SimpleAuthenticationInfo simpleAuthenticationInfo = (SimpleAuthenticationInfo)authInfo;
            if (!simpleAuthenticationInfo.hasHostAdminAccess()) {
               boolean isStation = isLocalConnection(req)
                  && NiagaraDaemon.getInstance().getStationRegistry().appRunning()
                  && simpleAuthenticationInfo.isExtra();
               if (authenticator != null && authenticator.getLog().isLoggable(Level.FINEST)) {
                  authenticator.getLog()
                     .finest(
                        "authAdmin authentication for request '"
                           + TextUtil.truncate(req.getRequestURI(), 25)
                           + "' is user but does not have host admin access (isStation = "
                           + isStation
                           + ")"
                     );
               }

               if (isStation) {
                  resp.setHeader("StationNotAdmin", "true");
               } else if (authenticator != null) {
                  authenticator.challenge(req, resp);
               }

               authInfoIsAdmin = false;
            } else {
               authInfoIsAdmin = true;
            }
         }
      }

      if (authenticator != null && authenticator.getLog().isLoggable(Level.FINEST)) {
         authenticator.getLog()
            .finest("authAdmin authentication for request '" + TextUtil.truncate(req.getRequestURI(), 25) + "' complete, authInfoIsAdmin = " + authInfoIsAdmin);
      }

      return authInfoIsAdmin;
   }

   public static boolean authUser(Authenticator authenticator, HttpServletRequest req, HttpServletResponse resp) {
      if (authenticator != null && authenticator.getLog().isLoggable(Level.FINEST)) {
         authenticator.getLog().finest("authUser authentication utility method used for request '" + TextUtil.truncate(req.getRequestURI(), 25) + "'");
      }

      boolean authInfoIsUser = false;
      Object authInfo = req.getAttribute("AuthenticationInfo");
      if (authInfo instanceof AuthenticationInfo) {
         if (authenticator != null && authenticator.getLog().isLoggable(Level.FINEST)) {
            authenticator.getLog()
               .finest(
                  "authUser authentication for request '"
                     + TextUtil.truncate(req.getRequestURI(), 25)
                     + "' found previously created attribute "
                     + "AuthenticationInfo"
                     + " value '"
                     + TextUtil.getClassName(authInfo.getClass())
                     + "'"
               );
         }

         authInfoIsUser = true;
      } else if (authenticator == NiagaraDaemon.getInstance().webServer.getAuthenticator()) {
         if (authenticator != null && authenticator.getLog().isLoggable(Level.FINEST)) {
            authenticator.getLog()
               .finest("authUser deferred to authenticator used in web server for request '" + TextUtil.truncate(req.getRequestURI(), 25) + "'");
         }

         authInfoIsUser = NiagaraDaemon.getInstance().webServer.authenticate(req, resp);
      } else if (authenticator == null) {
         authInfoIsUser = false;
      } else {
         AuthenticationInfo auth = authenticator.makeAuthInfo(req, resp);
         req.setAttribute("AuthenticationInfo", auth);
         authInfoIsUser = auth != null;
      }

      if (authenticator != null && authenticator.getLog().isLoggable(Level.FINEST)) {
         authenticator.getLog()
            .finest("authUser authentication for request '" + TextUtil.truncate(req.getRequestURI(), 25) + "' complete, authInfoIsUser = " + authInfoIsUser);
      }

      return authInfoIsUser;
   }

   public static boolean isNativeScheme(String scheme) {
      return scheme.toLowerCase().endsWith("native");
   }

   public static boolean isFileScheme(String scheme) {
      return scheme.toLowerCase().endsWith("file");
   }

   public static boolean isScramScheme(String scheme) {
      return scheme.toLowerCase().startsWith("scram");
   }

   public static boolean defaultCredentialsExist(IPlatformProvider platformProvider) {
      boolean defaultAccountExists = false;
      String authType = NiagaraDaemon.getInstance().auth.getAuthType();
      String defaultUsername = platformProvider.getDefaultUsername();
      Properties props = NiagaraDaemon.props;
      if (isNativeScheme(authType)) {
         boolean defaultLocal;
         if (props.containsKey("defaultlocal")) {
            defaultLocal = Boolean.parseBoolean(props.getProperty("defaultlocal", "true"));
         } else {
            defaultLocal = platformProvider.getComputerDomain(true) == null;
         }

         String defaultDomain = defaultLocal ? platformProvider.getComputerName() : platformProvider.getComputerDomain(true);
         if (platformProvider.getAccountFromName(defaultUsername, defaultDomain, true) != null) {
            defaultAccountExists = true;
         }
      } else if (isFileScheme(authType)) {
         String user = NiagaraDaemon.props.getProperty("user", null);
         if (user == null || user.equalsIgnoreCase(defaultUsername)) {
            defaultAccountExists = true;
         }
      }

      return defaultAccountExists;
   }

   public static boolean isLocalConnection(HttpServletRequest req) {
      try {
         return InetAddress.getByName(req.getLocalAddr()).isLoopbackAddress();
      } catch (UnknownHostException e) {
         return false;
      }
   }
}
