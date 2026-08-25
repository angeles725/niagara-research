package com.tridium.niagarad.http;

import com.tridium.niagarad.NiagaraDaemon;
import com.tridium.niagarad.security.AuthenticationDomain;
import com.tridium.niagarad.security.AuthenticationInfo;
import com.tridium.niagarad.security.os.NativeAuthenticationDomain;
import com.tridium.niagarad.util.CsrfTokenUtil;
import com.tridium.nre.auth.NativeAccount;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.nre.util.SecurityUtil;
import javax.baja.nre.util.TextUtil;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;

public final class BasicAuthenticator extends Authenticator {
   private static final String AUTH_SCHEME = "Basic";
   private static final String HTTP_SESSION_ATTR_BASIC = "basicServer";
   private final AuthenticationDomain authDomain;
   private final String authType;
   private static final Logger log = Logger.getLogger("auth.basic");

   public BasicAuthenticator(AuthenticationDomain authDomain) {
      this.authDomain = authDomain;
      this.authType = this.getAuthScheme().toLowerCase() + "/" + authDomain.getDomainType();
   }

   @Override
   public AuthenticationDomain getAuthDomain() {
      return this.authDomain;
   }

   @Override
   public String getAuthScheme() {
      return "Basic";
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
      label93: {
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
               break label93;
            }

            if (!(this.authDomain instanceof NativeAuthenticationDomain)) {
               if (log.isLoggable(Level.FINEST)) {
                  log.finest("unrecognized authentication domain '" + this.authDomain.getDomainType() + "', credentials not authenticated");
               }
               break label93;
            }

            authInfo = this.authDomain.makeAuthInfo(credentials.getUsername(), credentials.getPassword());
            if (authInfo == null) {
               log.finest("could not create authentication information for request, credentials not authenticated");
               break label93;
            }

            if (!this.validateExtraConditions(authInfo, req)) {
               log.finest("extra conditions not met");
               break label93;
            }

            session = this.createNewSession(req, true);
            if (log.isLoggable(Level.FINEST)) {
               log.finest("created new session '" + SecurityUtil.calculateSessionIdHash(session.getId()) + "' for authenticated request");
            }

            BasicAuthenticator.BasicServer authServer = new BasicAuthenticator.BasicServer();
            authServer.setAuthenticated(true);
            authServer.setAuthenticationInfo(authInfo);
            session.setAttribute("basicServer", authServer);
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
               break label93;
            }

            BasicAuthenticator.BasicServer authServer = (BasicAuthenticator.BasicServer)session.getAttribute("basicServer");
            if (authServer == null) {
               log.finest("server not found in session");
               break label93;
            }

            log.finest("found server");
            if (!authServer.isAuthenticated()) {
               log.finest("server not authenticated");
               break label93;
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
                  break label93;
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
               log.finest("authorization header present, rotated session to '" + SecurityUtil.calculateSessionIdHash(newSessionId) + "'");
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
      resp.setHeader("WWW-Authenticate", this.getAuthScheme() + " realm=\"" + this.authDomain.getRealm(req) + "\"");
   }

   private boolean parseUserData(HeaderField authorizationHeader, StringBuilder userName, StringBuilder password) {
      String value = authorizationHeader.attrs.getAtIndex(0);
      if (value == null) {
         return false;
      }

      byte[] userData;
      try {
         userData = Base64.getDecoder().decode(value);
      } catch (IllegalStateException ise) {
         return false;
      }

      String userDataString = new String(userData, StandardCharsets.UTF_8);
      int colonIndex = userDataString.indexOf(58);
      if (colonIndex == -1) {
         return false;
      }

      String tempUserName;
      try {
         tempUserName = userDataString.substring(0, colonIndex);
      } catch (IndexOutOfBoundsException ignored) {
         return false;
      }

      if (tempUserName != null && tempUserName.length() != 0) {
         userName.append(tempUserName);

         String tempPassword;
         try {
            tempPassword = userDataString.substring(colonIndex + 1);
         } catch (IndexOutOfBoundsException ignored) {
            return false;
         }

         if (tempPassword != null && tempPassword.length() != 0) {
            password.append(tempPassword);
            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   @Override
   public void sessionDestroyed(HttpSessionEvent se) {
      HttpSession session = se.getSession();
      if (session != null) {
         if (log.isLoggable(Level.FINEST)) {
            log.finest("session '" + SecurityUtil.calculateSessionIdHash(session.getId()) + "' destroyed, removing authenticator attributes");
         }

         session.removeAttribute("basicServer");
      }

      super.sessionDestroyed(se);
   }

   private static class BasicServer {
      private boolean authenticated = false;
      private AuthenticationInfo authenticationInfo;

      private BasicServer() {
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
