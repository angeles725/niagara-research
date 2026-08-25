package com.tridium.niagarad.http;

import com.tridium.niagarad.NiagaraDaemon;
import com.tridium.niagarad.security.AuthenticationDomain;
import com.tridium.niagarad.security.AuthenticationInfo;
import com.tridium.niagarad.security.SimpleAuthenticationDomain;
import com.tridium.niagarad.util.CsrfTokenUtil;
import com.tridium.niagarad.util.KeyedList;
import com.tridium.nre.auth.NativeAccount;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.nre.util.ByteBuffer;
import javax.baja.nre.util.SecurityUtil;
import javax.baja.nre.util.TextUtil;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;

public final class DigestAuthenticator extends Authenticator {
   private static final String AUTH_SCHEME = "Digest";
   private static final String HTTP_SESSION_ATTR_DIGEST = "digestServer";
   private static final int HASH_HEX_LENGTH = 32;
   private static final Object _MD5_MONITOR = new Object();
   private static MessageDigest md5 = null;
   private final AuthenticationDomain authDomain;
   private final String authType;
   private static final Logger log;

   public DigestAuthenticator(AuthenticationDomain authDomain) {
      this.authDomain = authDomain;
      this.authType = this.getAuthScheme().toLowerCase() + "/" + authDomain.getDomainType();
   }

   @Override
   public AuthenticationDomain getAuthDomain() {
      return this.authDomain;
   }

   @Override
   public String getAuthScheme() {
      return "Digest";
   }

   @Override
   public String getAuthType() {
      return this.authType;
   }

   @Override
   public Logger getLog() {
      return log;
   }

   @Override
   public AuthenticationInfo makeAuthInfo(HttpServletRequest req, HttpServletResponse resp) {
      HttpSession session;
      boolean challengeOnAuthFailure;
      Authenticator.RequestCredentials credentials;
      label97: {
         session = req.getSession(false);
         challengeOnAuthFailure = true;
         credentials = null;
         AuthenticationInfo authInfo;
         if (session == null) {
            if (log.isLoggable(Level.FINEST)) {
               log.finest("request for uri '" + TextUtil.truncate(req.getRequestURI(), 25) + "' does not have a session, request authentication required");
            }

            credentials = this.getRequestCredentials(req);
            if (credentials == null) {
               log.finest("credentials not found");
               break label97;
            }

            if (!this.digestIsValid(req, credentials)) {
               log.finest("could not complete the digest authentication process, credentials not authenticated");
               break label97;
            }

            if (!(this.authDomain instanceof SimpleAuthenticationDomain)) {
               if (log.isLoggable(Level.FINEST)) {
                  log.finest("unrecognized authentication domain '" + this.authDomain.getDomainType() + "', credentials not authenticated");
               }
               break label97;
            }

            authInfo = this.authDomain.makeAuthInfo(credentials.getUsername(), credentials.getPassword());
            if (authInfo == null) {
               log.finest("could not create authentication information for request, credentials not authenticated");
               break label97;
            }

            if (!this.validateExtraConditions(authInfo, req)) {
               log.finest("extra conditions not met");
               break label97;
            }

            session = this.createNewSession(req, true);
            if (log.isLoggable(Level.FINEST)) {
               log.finest("created new session '" + SecurityUtil.calculateSessionIdHash(session.getId()) + "' for authenticated request");
            }

            DigestAuthenticator.DigestServer authServer = new DigestAuthenticator.DigestServer();
            authServer.setAuthenticated(true);
            authServer.setAuthenticationInfo(authInfo);
            session.setAttribute("digestServer", authServer);
         } else {
            if (log.isLoggable(Level.FINEST)) {
               log.finest(
                  "request for uri '"
                     + TextUtil.truncate(req.getRequestURI(), 25)
                     + "' using session '"
                     + SecurityUtil.calculateSessionIdHash(session.getId())
                     + "'"
               );
            }

            if (this.detectSessionFixation(req, log)) {
               log.warning("possible session fixation detected, rejecting request and invalidating session");
               challengeOnAuthFailure = false;
               break label97;
            }

            DigestAuthenticator.DigestServer authServer = (DigestAuthenticator.DigestServer)session.getAttribute("digestServer");
            if (authServer == null) {
               log.finest("server not found in session");
               break label97;
            }

            log.finest("found server");
            if (!authServer.isAuthenticated()) {
               log.finest("server not authenticated");
               break label97;
            }

            authInfo = authServer.getAuthenticationInfo();
            credentials = this.getRequestCredentials(req);
            if (credentials != null) {
               String userName = credentials.getUsername();
               if (NativeAccount.isAccountNameFullyQualified(userName)) {
                  userName = NativeAccount.fullyQualifiedToUsername(userName);
               }

               if (!userName.equalsIgnoreCase(authInfo.getUsername())) {
                  log.finest("server authentication information incorrect");
                  break label97;
               }
            }

            log.finest("server authenticated");
         }

         if (!CsrfTokenUtil.csrfTokenExists(req)) {
            String csrfToken = CsrfTokenUtil.getCsrfToken(req);
            if (log.isLoggable(Level.FINEST)) {
               log.finest("adding CSRF token '" + csrfToken + "' to session '" + SecurityUtil.calculateSessionIdHash(session.getId()) + "'");
            }

            resp.setHeader("DaemonCSRFToken", csrfToken);
         }

         if (req.getHeader("Authorization") != null && !session.isNew()) {
            String newSessionId = req.changeSessionId();
            if (log.isLoggable(Level.FINEST)) {
               log.finest("authorization header present, rotated session id to '" + newSessionId + "'");
            }
         }

         NiagaraDaemon.getInstance().webServer.sessionAuthenticated(session, authInfo, req);
         log.finest("authentication completed successfully");
         return authInfo;
      }

      NiagaraDaemon.getInstance().webServer.sessionRejected(session, credentials != null ? credentials.getUsername() : "", req);
      if (challengeOnAuthFailure) {
         log.finest("client not authenticated, challenging");
         this.challenge(req, resp);
      } else {
         this.invalidateSession(req);
         Http.sendError(req, resp, 401);
      }

      return null;
   }

   @Override
   public Authenticator.RequestCredentials getRequestCredentials(HttpServletRequest req) {
      String authReqString = req.getHeader("Authorization");
      if (authReqString == null) {
         authReqString = req.getHeader("authorization");
      }

      if (authReqString == null) {
         return null;
      } else {
         HeaderField authReq = new HeaderField("Authorization: " + authReqString);
         if (!authReq.value.equalsIgnoreCase(this.getAuthScheme())) {
            return null;
         } else {
            StringBuilder username = new StringBuilder();
            StringBuilder passwordClearText = new StringBuilder();
            if (!this.parseUserData(authReq, username, passwordClearText)) {
               log.log(Level.FINEST, "failed to parse authorization header");
               return null;
            } else {
               return new Authenticator.RequestCredentials(username.toString(), passwordClearText.toString());
            }
         }
      }
   }

   @Override
   public void challenge(HttpServletRequest req, HttpServletResponse resp) {
      this.invalidateSession(req);
      resp.setHeader(
         "WWW-Authenticate",
         this.getAuthScheme() + " realm=\"" + this.authDomain.getRealm(req) + "\", qop=\"auth\", algorithm=\"MD5\", nonce=\"" + this.createNonce(req) + "\""
      );
   }

   private boolean parseUserData(HeaderField authorizationHeader, StringBuilder userName, StringBuilder password) {
      if (!authorizationHeader.attrs.containsKey("username")) {
         return false;
      }

      String tempUsername = getAttributeUnquoted(authorizationHeader.attrs, "username");
      if (tempUsername != null && tempUsername.length() != 0) {
         userName.append(tempUsername);
         if (this.authDomain == null) {
            return false;
         } else {
            String tempPassword = this.authDomain.getPasswordClearText(tempUsername);
            if (tempPassword != null && tempPassword.length() != 0) {
               password.append(tempPassword);
               return true;
            } else {
               return false;
            }
         }
      } else {
         return false;
      }
   }

   private String createNonce(HttpServletRequest req) {
      long now = System.currentTimeMillis();

      SecureRandom random;
      try {
         random = SecureRandom.getInstance("SHA1PRNG");
      } catch (NoSuchAlgorithmException e) {
         random = new SecureRandom();
      }

      byte[] digest = new byte[32];
      random.nextBytes(digest);
      this.getNonceDigest(req, now, digest);
      ByteBuffer buffer = new ByteBuffer();
      buffer.writeLong(now);
      buffer.write(digest);
      return new String(Base64.getEncoder().encode(buffer.toByteArray()));
   }

   private void getNonceDigest(HttpServletRequest req, long timestamp, byte[] digest) {
      ByteBuffer buffer = new ByteBuffer();
      buffer.writeLong(timestamp);
      buffer.writeBytes(this.authDomain.getRealm(req));
      buffer.writeInt(227);
      byte[] toHash;
      synchronized (_MD5_MONITOR) {
         toHash = md5.digest(buffer.toByteArray());
         md5.reset();
      }

      System.arraycopy(toHash, 0, digest, 0, toHash.length);
   }

   private String getExpectedResponse(HttpServletRequest req, String username, String password) {
      String authReqString = req.getHeader("Authorization");
      if (authReqString == null) {
         authReqString = req.getHeader("authorization");
      }

      HeaderField authReq = new HeaderField("Authorization: " + authReqString);
      String raw = username + ":" + this.authDomain.getRealm(req) + ":" + password;
      byte[] a1hash;
      synchronized (_MD5_MONITOR) {
         a1hash = md5.digest(raw.getBytes(StandardCharsets.UTF_8));
         md5.reset();
      }

      StringBuilder temp = new StringBuilder();

      for (byte anA1hash : a1hash) {
         temp.append(TextUtil.toLowerCase(TextUtil.byteToHexString(anA1hash)));
      }

      String a1HashString = temp.toString();
      String unquoted = getAttributeUnquoted(authReq.attrs, "uri");
      raw = req.getMethod() + ":" + unquoted;
      byte[] a2hash;
      synchronized (_MD5_MONITOR) {
         a2hash = md5.digest(raw.getBytes(StandardCharsets.UTF_8));
         md5.reset();
      }

      temp = new StringBuilder();

      for (byte anA2hash : a2hash) {
         temp.append(TextUtil.toLowerCase(TextUtil.byteToHexString(anA2hash)));
      }

      String a2HashString = temp.toString();
      String unqnonce = getAttributeUnquoted(authReq.attrs, "nonce");
      String unqcnonce = getAttributeUnquoted(authReq.attrs, "cnonce");
      String unqqop = getAttributeUnquoted(authReq.attrs, "qop");
      String nc = authReq.attrs.get("nc", null);
      raw = a1HashString + ":" + unqnonce + ":" + nc + ":" + unqcnonce + ":" + unqqop + ":" + a2HashString;
      byte[] result;
      synchronized (_MD5_MONITOR) {
         result = md5.digest(raw.getBytes(StandardCharsets.UTF_8));
         md5.reset();
      }

      temp = new StringBuilder();

      for (byte resultByte : result) {
         temp.append(TextUtil.toLowerCase(TextUtil.byteToHexString(resultByte)));
      }

      return temp.toString();
   }

   private boolean nonceIsValid(HttpServletRequest req) {
      byte[] digest = new byte[32];
      String authReqString = req.getHeader("Authorization");
      if (authReqString == null) {
         authReqString = req.getHeader("authorization");
      }

      if (authReqString == null) {
         return false;
      }

      HeaderField authReq = new HeaderField("Authorization: " + authReqString);
      String nonce = getAttributeUnquoted(authReq.attrs, "nonce");
      if (nonce == null) {
         return false;
      }

      byte[] decoded = Base64.getDecoder().decode(nonce);
      ByteBuffer decodedBuffer = new ByteBuffer(decoded);

      long timestamp;
      try {
         timestamp = decodedBuffer.readLong();
         decodedBuffer.read(digest);
      } catch (IOException e) {
         return false;
      }

      this.getNonceDigest(req, timestamp, digest);
      return decodedBuffer.endsWith(digest);
   }

   private boolean digestIsValid(HttpServletRequest req, Authenticator.RequestCredentials credentials) {
      String authReqString = req.getHeader("Authorization");
      if (authReqString == null) {
         log.finest("authorization header not found");
         return false;
      } else {
         HeaderField authReq = new HeaderField("Authorization: " + authReqString);
         boolean failed = !authReq.value.equalsIgnoreCase(this.getAuthScheme());
         failed |= !compareUnquoted(authReq.attrs, "realm", this.authDomain.getRealm(req));
         failed |= !this.nonceIsValid(req);
         failed |= !compareUnquoted(authReq.attrs, "qop", "auth");
         failed |= !authReq.attrs.containsKey("nc");
         failed |= !authReq.attrs.containsKey("cnonce");
         failed |= !authReq.attrs.containsKey("uri");
         failed |= !authReq.attrs.containsKey("response");
         failed |= !compareUnquoted(authReq.attrs, "response", this.getExpectedResponse(req, credentials.getUsername(), credentials.getPassword()));
         return !failed;
      }
   }

   private static boolean compareUnquoted(KeyedList attrs, String attrName, String value) {
      String actualValue = attrs.get(attrName, "");
      String unquoted = TextUtil.replace(actualValue, "\"", "");
      return value.equals(unquoted);
   }

   private static String getAttributeUnquoted(KeyedList attrs, String attrName) {
      String actualValue = attrs.get(attrName, "");
      return actualValue.isEmpty() ? null : TextUtil.replace(actualValue, "\"", "");
   }

   @Override
   public void sessionDestroyed(HttpSessionEvent se) {
      HttpSession session = se.getSession();
      if (session != null) {
         if (log.isLoggable(Level.FINEST)) {
            log.finest("session '" + SecurityUtil.calculateSessionIdHash(session.getId()) + "' destroyed, removing authenticator attributes");
         }

         session.removeAttribute("digestServer");
      }

      super.sessionDestroyed(se);
   }

   static {
      try {
         md5 = MessageDigest.getInstance("MD5");
      } catch (NoSuchAlgorithmException nsae) {
         System.err.println("SEVERE [" + new Date() + "] [auth.digest] failed to initialize MD5 authentication algorithm (" + nsae + ")");
         nsae.printStackTrace();
      }

      log = Logger.getLogger("auth.digest");
   }

   private static class DigestServer {
      private boolean authenticated = false;
      private AuthenticationInfo authenticationInfo;

      private DigestServer() {
      }

      public void setAuthenticated(boolean authenticated) {
         this.authenticated = authenticated;
      }

      public boolean isAuthenticated() {
         return this.authenticated;
      }

      public void setAuthenticationInfo(AuthenticationInfo authenticationInfo) {
         this.authenticationInfo = authenticationInfo;
      }

      public AuthenticationInfo getAuthenticationInfo() {
         return this.authenticationInfo;
      }
   }
}
