package com.tridium.niagarad.http;

import com.tridium.niagarad.NiagaraDaemon;
import com.tridium.niagarad.security.AuthenticationDomain;
import com.tridium.niagarad.security.AuthenticationInfo;
import com.tridium.niagarad.security.SimpleAuthenticationInfo;
import com.tridium.niagarad.util.DaemonAuthUtil;
import com.tridium.nre.platform.IPlatformProvider;
import com.tridium.nre.security.EncryptionAlgorithmBundle;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Properties;
import java.util.Queue;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.nre.util.SecurityUtil;
import javax.baja.nre.util.TextUtil;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

public abstract class Authenticator implements HttpSessionListener {
   private static final Object SESSION_LIMIT_LOCK = new Object();
   private final Queue<HttpSession> authenticatedSessionList = new LinkedList<>();
   private final Queue<HttpSession> unauthenticatedSessionList = new LinkedList<>();
   protected static final String HTTP_SESSION_LAST_REMOTE_ADDRESS = "lastRemoteAddress";
   protected static final String HTTP_SESSION_LAST_USER_AGENT = "lastUserAgent";
   protected static final String HTTP_SESSION_AUDITED = "sessionAudited";
   private static ArrayList<HttpServletRequest> exemptFromExtraConditionsRequests = new ArrayList<>();

   public static Authenticator make(Logger handler, Properties props, Properties extraUsers, Properties extraAdminUsers, IPlatformProvider platformProvider) {
      String defaultAuthScheme = DaemonAuthUtil.getDefaultAuthScheme(platformProvider);
      String authType = props.getProperty("authtype", defaultAuthScheme);
      Set<String> platformAuthTypes = new HashSet<>();
      String platformAuthTypeString = platformProvider.getSupportedAuthenticationTypes();
      Collections.addAll(platformAuthTypes, platformAuthTypeString.split(","));
      Set<String> knownSupportedAuthTypes = new HashSet<>();
      if (platformAuthTypes.remove("scram-sha512/native")) {
         knownSupportedAuthTypes.add("scram-sha512/native");
      }

      if (platformAuthTypes.remove("scram-glibc-sha256/native")) {
         knownSupportedAuthTypes.add("scram-glibc-sha256/native");
      }

      if (platformAuthTypes.remove("scram-glibc-sha512/native")) {
         knownSupportedAuthTypes.add("scram-glibc-sha512/native");
      }

      if (platformAuthTypes.remove("scram-bcrypt/native")) {
         knownSupportedAuthTypes.add("scram-bcrypt/native");
      }

      if (platformAuthTypes.remove("basic/native")) {
         knownSupportedAuthTypes.add("basic/native");
      }

      if (platformAuthTypes.remove("scram-glibc-sha512/file")) {
         knownSupportedAuthTypes.add("scram-glibc-sha512/file");
      }

      for (String platformAuthType : platformAuthTypes) {
         handler.warning("platform supports unrecognized authtype \"" + platformAuthType + "\"");
      }

      if (!knownSupportedAuthTypes.contains(authType)) {
         handler.severe("unsupported authtype \"" + authType + "\" specified, defaulting to \"" + defaultAuthScheme + "\"");
         authType = defaultAuthScheme;
         props.setProperty("authtype", authType);
         if (DaemonAuthUtil.isFileScheme(authType)) {
            props.remove("user");
            props.remove("password");
         }

         NiagaraDaemon.saveProperties();
      }

      AuthenticationDomain domain = AuthenticationDomain.make(props, extraUsers, extraAdminUsers, platformProvider);
      if (domain == null) {
         handler.severe("failed to create authentication domain in authenticator factory, returning null");
         return null;
      }

      switch (authType) {
         case "scram-sha512/native":
         case "scram-glibc-sha256/native":
         case "scram-glibc-sha512/native":
         case "scram-bcrypt/native":
         case "scram-glibc-sha512/file":
            if (domain.supportsPasswordHashRetrieval()) {
               String hashMechanism = TextUtil.split(authType, '/')[0];
               return new ScramAuthenticator(domain, hashMechanism, platformProvider);
            }

            handler.severe("authtype is \"" + authType + "\" but domain does not support password hash retrieval");
            return null;
         case "basic/native":
            return new BasicAuthenticator(domain);
         default:
            return null;
      }
   }

   protected Authenticator() {
   }

   boolean validateExtraConditions(AuthenticationInfo authInfo, HttpServletRequest request) {
      if (authInfo instanceof SimpleAuthenticationInfo && ((SimpleAuthenticationInfo)authInfo).isExtra()) {
         try {
            if (!InetAddress.getByName(request.getLocalAddr()).isLoopbackAddress()) {
               return false;
            }
         } catch (UnknownHostException var4) {
         }

         if (!exemptFromExtraConditionsRequests.contains(request) && !NiagaraDaemon.getInstance().getStationRegistry().appRunning()) {
            return false;
         }
      }

      return true;
   }

   public abstract AuthenticationInfo makeAuthInfo(HttpServletRequest var1, HttpServletResponse var2);

   public String getRequestUserName(HttpServletRequest req) {
      Authenticator.RequestCredentials credentials = this.getRequestCredentials(req);
      return credentials == null ? null : credentials.getUsername();
   }

   public abstract Authenticator.RequestCredentials getRequestCredentials(HttpServletRequest var1);

   public abstract void challenge(HttpServletRequest var1, HttpServletResponse var2);

   public abstract String getAuthType();

   public abstract AuthenticationDomain getAuthDomain();

   public abstract String getAuthScheme();

   public abstract Logger getLog();

   public boolean supportsSecureKeyExchange() {
      return false;
   }

   public boolean keyExchangeEnabled(HttpSession session) {
      return false;
   }

   public byte[] extractSessionKey(HttpSession session) {
      return null;
   }

   public EncryptionAlgorithmBundle getEncryptionAlgorithmBundle(HttpSession session) {
      return null;
   }

   public void sessionCreated(HttpSessionEvent se) {
   }

   public void sessionDestroyed(HttpSessionEvent se) {
      HttpSession session = se.getSession();
      if (session != null) {
         synchronized (SESSION_LIMIT_LOCK) {
            this.authenticatedSessionList.remove(session);
            this.unauthenticatedSessionList.remove(session);
         }

         session.removeAttribute("lastRemoteAddress");
         session.removeAttribute("lastUserAgent");
         session.removeAttribute("sessionAudited");
      }
   }

   protected void promoteSessionReference(HttpSession session) {
      synchronized (SESSION_LIMIT_LOCK) {
         if (this.unauthenticatedSessionList.remove(session)) {
            this.authenticatedSessionList.add(session);
            this.removeOldestIfFull(this.authenticatedSessionList, true);
         }
      }
   }

   protected boolean detectSessionFixation(HttpServletRequest httpServletRequest, Logger log) {
      boolean suspectFixation = false;
      HttpSession httpSession = httpServletRequest.getSession(false);
      if (httpSession != null && !httpSession.isNew()) {
         String lastUserAgent = (String)httpSession.getAttribute("lastUserAgent");
         if (lastUserAgent != null) {
            if (!lastUserAgent.equals(httpServletRequest.getHeader("user-agent"))) {
               log.warning(
                  "session '"
                     + SecurityUtil.calculateSessionIdHash(httpSession.getId())
                     + "' last user agent '"
                     + lastUserAgent
                     + "' did not match request user agent '"
                     + httpServletRequest.getHeader("user-agent")
                     + "'"
               );
               suspectFixation = true;
            } else if (!httpServletRequest.isSecure()) {
               String lastRemoteAddress = (String)httpSession.getAttribute("lastRemoteAddress");
               if (lastRemoteAddress != null && !lastRemoteAddress.equals(httpServletRequest.getRemoteAddr())) {
                  log.warning(
                     "session '"
                        + SecurityUtil.calculateSessionIdHash(httpSession.getId())
                        + "' last remote address '"
                        + lastRemoteAddress
                        + "' did not match request remote address '"
                        + httpServletRequest.getRemoteAddr()
                        + "'"
                  );
                  suspectFixation = true;
               }
            }
         }
      }

      if (httpSession != null && suspectFixation) {
         log.warning(
            "possible session fixation detected from '"
               + httpServletRequest.getRemoteAddr()
               + ":"
               + httpServletRequest.getRemotePort()
               + "' with user-agent '"
               + httpServletRequest.getHeader("user-agent")
               + "' on session '"
               + SecurityUtil.calculateSessionIdHash(httpSession.getId())
               + "' with request '"
               + httpServletRequest.getMethod()
               + " "
               + httpServletRequest.getRequestURI()
               + "'"
         );
      }

      return suspectFixation;
   }

   protected HttpSession createNewSession(HttpServletRequest req, boolean authenticated) {
      HttpSession session = req.getSession();
      synchronized (SESSION_LIMIT_LOCK) {
         Queue<HttpSession> map = authenticated ? this.authenticatedSessionList : this.unauthenticatedSessionList;
         map.add(session);
         this.removeOldestIfFull(map, authenticated);
         session.setAttribute("lastRemoteAddress", req.getRemoteAddr());
         session.setAttribute("lastUserAgent", req.getHeader("user-agent"));
         return session;
      }
   }

   private void removeOldestIfFull(Queue<HttpSession> toCheck, boolean logRemoval) {
      if (toCheck.size() > WebServer.getJettyMaxHttpSessions()) {
         HttpSession oldestSession = toCheck.remove();
         if (logRemoval && this.getLog().isLoggable(Level.FINE)) {
            this.getLog()
               .fine("maximum session count reached, invalidating oldest session '" + SecurityUtil.calculateSessionIdHash(oldestSession.getId()) + "'");
         }

         NiagaraDaemon.getInstance().webServer.sessionInvalidated(oldestSession);
      }
   }

   protected void invalidateSession(HttpServletRequest req) {
      HttpSession requestSession = req.getSession(false);
      if (requestSession != null) {
         if (this.getLog().isLoggable(Level.FINE)) {
            this.getLog()
               .finest(
                  "authentication failure occurred in session '"
                     + SecurityUtil.calculateSessionIdHash(requestSession.getId())
                     + "' while handling request for uri '"
                     + TextUtil.truncate(req.getRequestURI(), 25)
                     + "', invalidating session"
               );
         }

         NiagaraDaemon.getInstance().webServer.sessionInvalidated(requestSession);
      }
   }

   public static void addRunningStationExemption(HttpServletRequest request) {
      exemptFromExtraConditionsRequests.add(request);
   }

   public static void removeRunningStationExemption(HttpServletRequest request) {
      exemptFromExtraConditionsRequests.remove(request);
   }

   protected static class RequestCredentials {
      private String username;
      private String password;

      private RequestCredentials() {
      }

      RequestCredentials(String username, String password) {
         this.username = username;
         this.password = password;
      }

      public String getUsername() {
         return this.username;
      }

      public String getPassword() {
         return this.password;
      }
   }
}
