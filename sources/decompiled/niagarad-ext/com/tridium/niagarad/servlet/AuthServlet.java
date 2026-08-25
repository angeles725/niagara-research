package com.tridium.niagarad.servlet;

import com.tridium.niagarad.NiagaraDaemon;
import com.tridium.niagarad.http.Http;
import com.tridium.niagarad.log.ErrorHandler;
import com.tridium.niagarad.log.MessageBundle;
import com.tridium.niagarad.log.SimpleErrorHandler;
import com.tridium.niagarad.security.AuthenticationDomain;
import com.tridium.niagarad.security.AuthenticationInfo;
import com.tridium.niagarad.security.os.NativeAuthenticationInfo;
import com.tridium.niagarad.util.CsrfTokenUtil;
import com.tridium.niagarad.util.DaemonAuthUtil;
import com.tridium.niagarad.util.KeyedList;
import com.tridium.nre.auth.GroupAccount;
import com.tridium.nre.auth.UserAccount;
import com.tridium.nre.platform.IPlatformProvider;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import javax.baja.nre.security.SharedSecretKey;
import javax.baja.nre.util.ByteBuffer;
import javax.baja.xml.XWriter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class AuthServlet extends Servlet {
   private final IPlatformProvider platformProvider;

   public AuthServlet(IPlatformProvider platformProvider) {
      super("auth");
      this.platformProvider = platformProvider;
   }

   @Override
   public void doGet(HttpServletRequest req, HttpServletResponse resp) {
      String queryString = req.getQueryString();
      ErrorHandler errorHandler = new SimpleErrorHandler();
      ByteBuffer buffer = new ByteBuffer();
      XWriter content = new XWriter();
      content.setOutputStream(buffer.getOutputStream());
      if (queryString == null) {
         resp.setStatus(this.doGet(errorHandler, (AuthenticationInfo)req.getAttribute("AuthenticationInfo"), new KeyedList(), content, req.getSession(false)));
      } else {
         KeyedList query = Http.getGetForm(queryString);
         resp.setStatus(this.doGet(errorHandler, (AuthenticationInfo)req.getAttribute("AuthenticationInfo"), query, content, req.getSession(false)));
      }

      content.flush();
      if (resp.getStatus() > 299 && errorHandler.getLastError() != null) {
         Http.sendErrorXML(req, resp, resp.getStatus(), errorHandler.getLastError());
      } else if (buffer.toByteArray().length == 0) {
         content.prolog();
         content.w("<success ").attr("statusCode", String.valueOf(resp.getStatus())).w("/>\n");
         content.flush();
         byte[] bytes = buffer.toByteArray();
         resp.setHeader("Content-Type", "text/xml");
         resp.setIntHeader("Content-Length", bytes.length);

         try {
            resp.getOutputStream().write(bytes);
         } catch (IOException ioe) {
            this.getServer().getFilter().log(Level.SEVERE, this.getName() + ": failed to write auth response (" + ioe + ")", ioe);
            Http.sendError(req, resp, 500);
         }
      } else {
         byte[] bytes = buffer.toByteArray();
         resp.setHeader("Content-Type", "text/xml");
         resp.setIntHeader("Content-Length", bytes.length);

         try {
            resp.getOutputStream().write(bytes);
         } catch (IOException ioe) {
            if (this.getServer() != null && this.getServer().getState() == 1) {
               this.getServer().getFilter().log(Level.SEVERE, this.getName() + ": failed to write auth response (" + ioe + ")", ioe);
               Http.sendError(req, resp, 500);
            }
         }
      }

      content.close();
   }

   public int doGet(ErrorHandler handler, AuthenticationInfo reqUser, KeyedList query, XWriter content, HttpSession session) {
      if (query.containsKey("update")) {
         if (!DebugServlet.debugEnabled && !CsrfTokenUtil.verifyCsrfToken(session, query.get("csrfToken", null))) {
            MessageBundle msg = new MessageBundle("invalid CSRF token in request");
            handler.error(msg);
            return 403;
         } else {
            return this.doUpdate(handler, query, session);
         }
      } else {
         if (query.containsKey("checkDefault")) {
            return this.doCheckDefault(content, this.platformProvider);
         }

         if (query.containsKey("checkReadonly")) {
            return this.doCheckReadonly(content, this.platformProvider);
         }

         Set<String> supportedAuthTypes = new HashSet<>();
         String supportedAuthTypeString = this.platformProvider.getSupportedAuthenticationTypes();
         if (supportedAuthTypeString.contains("basic/native")) {
            supportedAuthTypes.add("basic/native");
         }

         if (supportedAuthTypeString.contains("scram-sha512/native")) {
            supportedAuthTypes.add("scram-sha512/native");
         }

         if (supportedAuthTypeString.contains("scram-bcrypt/native")) {
            supportedAuthTypes.add("scram-bcrypt/native");
         }

         if (supportedAuthTypeString.contains("scram-glibc-sha256/native")) {
            supportedAuthTypes.add("scram-glibc-sha256/native");
         }

         if (supportedAuthTypeString.contains("scram-glibc-sha512/native")) {
            supportedAuthTypes.add("scram-glibc-sha512/native");
         }

         if (supportedAuthTypeString.contains("scram-glibc-sha512/file")) {
            supportedAuthTypes.add("scram-glibc-sha512/file");
         }

         String authType = NiagaraDaemon.getInstance().auth.getAuthType();
         content.w("<authInfo>\n");
         content.w("<auth")
            .w(' ')
            .attr("type", authType)
            .w(' ')
            .attr("authTypesSupported", String.join(",", supportedAuthTypes))
            .w(' ')
            .attr("readonly", String.valueOf(this.platformProvider.isAuthenticationReadonly()))
            .w(">\n");
         Properties props = NiagaraDaemon.props;
         if (DaemonAuthUtil.isNativeScheme(authType)) {
            boolean defaultLocal;
            if (props.containsKey("defaultlocal")) {
               defaultLocal = Boolean.valueOf(props.getProperty("defaultlocal", "true"));
            } else {
               defaultLocal = this.platformProvider.getComputerDomain(true) == null;
            }

            String defaultDomain = defaultLocal ? this.platformProvider.getComputerName() : this.platformProvider.getComputerDomain(true);
            content.w("<defaultlocal").w(' ').attr("value", String.valueOf(defaultLocal)).w(' ').attr("realm", defaultDomain).w("/>");
            String adminGroupIdProp = "";
            if (!this.platformProvider.providesAccountManagement()) {
               adminGroupIdProp = props.getProperty("admingroupid", "");
            }

            GroupAccount[] adminGroups = this.platformProvider.getAccounts(adminGroupIdProp, ',');
            if (adminGroups == null || adminGroups.length == 0) {
               adminGroups = new GroupAccount[]{this.platformProvider.getDefaultAdminGroup()};
            }

            for (GroupAccount adminGroup : adminGroups) {
               content.w("<admingroup").w(' ').attr("name", adminGroup.getFullyQualifiedName()).w(' ').attr("id", adminGroup.getPlatformIdentifier()).w("/>");
            }

            String defaultUsername = this.platformProvider.getDefaultUsername();
            if (this.getServer().getServlet("acctmgt") instanceof AccountManagementServlet) {
               content.w("<user").w(' ').attr("name", defaultUsername);
               if (this.platformProvider.getAccountFromName(defaultUsername, defaultDomain, true) != null) {
                  content.w(' ').attr("default", "true");
               }

               content.w("/>");
            } else {
               content.w("<user").w(' ').attr("name", defaultUsername);
               AuthenticationDomain authDomain = this.getServer().getAuthenticator().getAuthDomain();
               if (authDomain.makeAuthInfo(defaultUsername, this.platformProvider.getDefaultPassword()) != null) {
                  content.w(' ').attr("default", "true");
               }

               content.w("/>");
            }
         } else if (DaemonAuthUtil.isFileScheme(authType)) {
            String user = NiagaraDaemon.props.getProperty("user", null);
            if (user == null) {
               user = this.platformProvider.getDefaultUsername();
            }

            content.w("<user ").attr("name", user);
            AuthenticationDomain authDomain = this.getServer().getAuthenticator().getAuthDomain();
            if (authDomain.makeAuthInfo(this.platformProvider.getDefaultUsername(), this.platformProvider.getDefaultPassword()) != null) {
               content.w(' ').attr("default", "true");
            }

            content.w("/>");
         }

         content.w("</auth>\n");
         if (!Boolean.valueOf(query.get("sendDomain", "true"))) {
            content.w("</authInfo>\n");
            return 200;
         }

         if (supportedAuthTypes.contains("basic/native")
            || supportedAuthTypes.contains("scram-sha512/native")
            || supportedAuthTypes.contains("scram-glibc-sha256/native")
            || supportedAuthTypes.contains("scram-glibc-sha512/native")
            || supportedAuthTypes.contains("scram-bcrypt/native")) {
            boolean defaultLocal;
            if (props.containsKey("defaultlocal")) {
               defaultLocal = Boolean.valueOf(props.getProperty("defaultlocal", "true"));
            } else {
               defaultLocal = this.platformProvider.getComputerDomain(true) == null;
            }

            if (query.containsKey("reqUsername")) {
               String sharedKeyAttributeName = "sharedKey_" + query.get("sharedKeyName", null);
               SharedSecretKey sharedSecretKey = (SharedSecretKey)session.getAttribute(sharedKeyAttributeName);
               if (sharedSecretKey != null) {
                  String passwordCipher = query.get("reqPassword", "");

                  try {
                     String username = sharedSecretKey.decrypt(Base64.getDecoder().decode(query.get("reqUsername", ""))).asString(true, StandardCharsets.UTF_8);
                     String passwordPlain;
                     if (!passwordCipher.isEmpty()) {
                        passwordPlain = sharedSecretKey.decrypt(Base64.getDecoder().decode(passwordCipher)).asString(true, StandardCharsets.UTF_8);
                     } else {
                        passwordPlain = "";
                     }

                     UserAccount acct = this.platformProvider.getAccountFromCredentials(username, passwordPlain, defaultLocal);
                     if (acct != null) {
                        String xml = this.platformProvider.getDomainGroupsXml(acct.getPlatformIdentifier());
                        if (xml != null) {
                           content.write(xml);
                        }
                     }
                  } catch (Exception var18) {
                  }
               }
            } else if (DaemonAuthUtil.isNativeScheme(authType) && reqUser instanceof NativeAuthenticationInfo) {
               content.write(this.platformProvider.getDomainGroupsXml(((NativeAuthenticationInfo)reqUser).getIdentity().getAccount().getPlatformIdentifier()));
            }
         }

         content.w("</authInfo>\n");
         return 200;
      }
   }

   @Override
   public boolean useDefaultAuthentication() {
      return false;
   }

   @Override
   public boolean authenticate(HttpServletRequest req, HttpServletResponse resp) {
      boolean requireAdmin = false;
      String queryString = req.getQueryString();
      if (queryString != null) {
         KeyedList query = Http.getGetForm(queryString);
         requireAdmin = query.containsKey("update");
      }

      return requireAdmin
         ? DaemonAuthUtil.authAdmin(this.getServer().getAuthenticator(), req, resp)
         : DaemonAuthUtil.authUser(this.getServer().getAuthenticator(), req, resp);
   }

   private int doCheckReadonly(XWriter content, IPlatformProvider platformProvider) {
      content.w("<authInfo>\n");
      content.w("<auth").w(' ').attr("readonly", String.valueOf(platformProvider.isAuthenticationReadonly())).w("/>\n");
      content.w("</authInfo>\n");
      return 200;
   }

   private int doCheckDefault(XWriter content, IPlatformProvider platformProvider) {
      String authType = NiagaraDaemon.getInstance().auth.getAuthType();
      content.w("<authInfo>\n");
      content.w("<auth>\n");
      Properties props = NiagaraDaemon.props;
      if (DaemonAuthUtil.isNativeScheme(authType)) {
         boolean defaultLocal;
         if (props.containsKey("defaultlocal")) {
            defaultLocal = Boolean.valueOf(props.getProperty("defaultlocal", "true"));
         } else {
            defaultLocal = platformProvider.getComputerDomain(true) == null;
         }

         String defaultDomain = defaultLocal ? platformProvider.getComputerName() : platformProvider.getComputerDomain(true);
         String defaultUsername = platformProvider.getDefaultUsername();
         if (this.getServer().getServlet("acctmgt") instanceof AccountManagementServlet) {
            content.w("<user").w(' ').attr("name", defaultUsername);
            if (platformProvider.getAccountFromName(defaultUsername, defaultDomain, true) != null) {
               content.w(' ').attr("default", "true");
            }

            content.w("/>\n");
         } else {
            content.w("<user").w(' ').attr("name", defaultUsername);
            AuthenticationDomain authDomain = this.getServer().getAuthenticator().getAuthDomain();
            if (authDomain.makeAuthInfo(defaultUsername, platformProvider.getDefaultPassword()) != null) {
               content.w(' ').attr("default", "true");
            }

            content.w("/>\n");
         }
      } else if (DaemonAuthUtil.isFileScheme(authType)) {
         String user = NiagaraDaemon.props.getProperty("user", null);
         if (user == null) {
            user = platformProvider.getDefaultUsername();
         }

         content.w("<user ").attr("name", user);
         AuthenticationDomain authDomain = this.getServer().getAuthenticator().getAuthDomain();
         if (authDomain.makeAuthInfo(platformProvider.getDefaultUsername(), platformProvider.getDefaultPassword()) != null) {
            content.w(' ').attr("default", "true");
         }

         content.w("/>\n");
      }

      content.w("</auth>\n");
      content.w("</authInfo>\n");
      return 200;
   }

   private int doUpdate(ErrorHandler handler, KeyedList query, HttpSession session) {
      if (this.platformProvider.isAuthenticationReadonly()) {
         MessageBundle msg = new MessageBundle("platform", "AuthServlet.readonly", "AuthServlet: Authentication settings are readonly");
         handler.error(msg);
         this.getServer().getFilter().severe("authentication settings are readonly");
         return 400;
      }

      if (query.containsKey("authtype")) {
         String testValue = query.get("authtype", "");
         boolean invalid = testValue.trim().isEmpty();
         invalid |= !this.platformProvider.getSupportedAuthenticationTypes().contains(testValue);
         if (invalid) {
            MessageBundle msg = new MessageBundle("platform", "AuthServlet.authType", "AuthServlet: Invalid authentication type");
            handler.error(msg);
            return 400;
         }
      }

      if (query.containsKey("user")) {
         String testValue = query.get("user", "");
         if (testValue.trim().isEmpty()) {
            MessageBundle msg = new MessageBundle("platform", "AuthServlet.badUser", "AuthServlet: Invalid user name");
            handler.error(msg);
            return 400;
         }

         if (testValue.trim().equals(this.platformProvider.getDefaultUsername())) {
            MessageBundle msg = new MessageBundle("platform", "AuthServlet.badUser", "AuthServlet: Invalid user name");
            handler.error(msg);
            return 400;
         }
      }

      String decodedPasswordValue = null;
      if (query.containsKey("password")) {
         try {
            String sharedKeyAttributeName = "sharedKey_" + query.get("sharedKeyName", null);
            SharedSecretKey sharedKey = (SharedSecretKey)session.getAttribute(sharedKeyAttributeName);
            String encodedPasswordValue = query.get("password", null);
            decodedPasswordValue = sharedKey.decrypt(Base64.getDecoder().decode(encodedPasswordValue)).asString(true, StandardCharsets.UTF_8);
            if (decodedPasswordValue.trim().isEmpty()) {
               throw new Exception();
            }

            if (decodedPasswordValue.equals(this.platformProvider.getDefaultPassword())) {
               throw new Exception();
            }
         } catch (Exception e) {
            MessageBundle msg = new MessageBundle("platform", "AuthServlet.password", "AuthServlet: Invalid password");
            handler.error(msg);
            return 400;
         }
      }

      if (query.containsKey("admingroupid")) {
         String testValue = query.get("admingroupid", "");
         if (this.platformProvider.getAccountFromId(testValue, false) == null) {
            MessageBundle msg = new MessageBundle("platform", "AuthServlet.badGroup", "AuthServlet: Invalid group ID");
            handler.error(msg);
            return 400;
         }
      }

      copyProperty(query, "authtype");
      copyProperty(query, "user");
      if (decodedPasswordValue != null) {
         NiagaraDaemon.props.setProperty("password", decodedPasswordValue);
      }

      if (!this.platformProvider.providesAccountManagement()) {
         copyProperty(query, "defaultlocal");
         copyProperty(query, "admingroupid");
      }

      String authType = NiagaraDaemon.props.getProperty("authtype");
      if (DaemonAuthUtil.isNativeScheme(authType)) {
         NiagaraDaemon.props.remove("user");
         NiagaraDaemon.props.remove("password");
         NiagaraDaemon.props.remove("_password");
      } else {
         NiagaraDaemon.props.remove("defaultlocal");
         NiagaraDaemon.props.remove("admingroupid");
      }

      NiagaraDaemon.saveProperties();
      NiagaraDaemon.getInstance().updateAuthenticator();
      return 200;
   }

   private static void copyProperty(KeyedList query, String propName) {
      String propValue = query.get(propName, null);
      if (propValue != null && !propValue.trim().isEmpty()) {
         NiagaraDaemon.props.setProperty(propName, propValue);
      }
   }
}
