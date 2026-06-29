package com.tridium.opcUaServer.util;

import com.prosysopc.ua.StatusException;
import com.prosysopc.ua.server.ServerUserIdentity;
import com.prosysopc.ua.server.Session;
import com.prosysopc.ua.server.UserValidator;
import com.prosysopc.ua.stack.builtintypes.NodeId;
import com.prosysopc.ua.stack.builtintypes.StatusCode;
import com.prosysopc.ua.stack.core.UserIdentityToken;
import com.prosysopc.ua.stack.core.UserTokenType;
import com.tridium.authn.BAuthenticationService;
import com.tridium.authn.LoginFailureCause;
import com.tridium.opcUaServer.BOpcUaServerSession;
import com.tridium.opcUaServer.authn.BOpcUaCallbackHandler;
import com.tridium.session.SessionManager;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.authn.BAuthenticationScheme;
import javax.baja.security.AuthenticationException;
import javax.baja.sys.Sys;
import javax.baja.user.BUser;
import javax.baja.user.BUserService;
import javax.baja.util.Lexicon;

public class OpcUaUserValidator implements UserValidator {
   private BOpcUaServerSession opcUaSession;
   private static final HashMap<String, OpcUaUserValidator.SessionData> nodeIdSessionDataHashMap = new HashMap<>();
   private final Logger logger = Logger.getLogger("opcUaServer.userValidator");
   private static final Lexicon lex = Lexicon.make(OpcUaUserValidator.class);
   private final OpcUaServerCertificateValidator certificateValidator;

   public OpcUaUserValidator(OpcUaServerCertificateValidator certificateValidator) {
      this.certificateValidator = certificateValidator;
   }

   public boolean onValidate(Session session, ServerUserIdentity userIdentity) throws StatusException {
      if (this.logger.isLoggable(Level.FINE)) {
         this.logger.fine("onValidate: userIdentity.type = " + userIdentity.getType());
      }

      if (userIdentity.getType() == UserTokenType.UserName) {
         try {
            BUser requestedUser = BUserService.getService().getUser(userIdentity.getName());
            BUser authenticatedUser = this.validateOpcUaUser(userIdentity, session);
            if (authenticatedUser == null || !authenticatedUser.equals(requestedUser)) {
               this.logger.warning(lex.getText("opcUaUserAuth.failure", new Object[]{requestedUser.getName()}));
               invalidateSessionForNodeId(session.getSessionId());
               return false;
            }
         } catch (AuthenticationException var5) {
            this.logger.warning(var5.getLocalizedMessage());
            invalidateSessionForNodeId(session.getSessionId());
            return false;
         }
      } else if (userIdentity.getType() == UserTokenType.Certificate) {
         StatusCode code = this.certificateValidator.validateCertificate(userIdentity.getCertificate());
         if (code.isGood()) {
            return true;
         }

         this.logger
            .warning(lex.getText("opcUaCertAuth.failure", new Object[]{userIdentity.getCertificate().getCertificate().getSubjectX500Principal().getName()}));
         return false;
      }

      return true;
   }

   private BUser validateOpcUaUser(ServerUserIdentity userIdentity, Session session) {
      String userName = userIdentity.getName();
      String password = userIdentity.getPassword();
      String authScheme = lex.getText("opcUaAuthScheme");
      this.opcUaSession = BOpcUaServerSession.make(session);
      BUser requestedUser = BUserService.getService().getUser(userName);
      if (requestedUser == null) {
         this.logger.severe(lex.getText("opcUaUser.invalid", new Object[]{userName}));
         return null;
      } else {
         BAuthenticationScheme authnScheme = requestedUser.getAuthenticationScheme();
         if (!authnScheme.getSchemeName().equalsIgnoreCase(authScheme)) {
            this.logger.severe(LoginFailureCause.LOGIN_INTERFACE_NOT_SUPPORTED.toString());
            return null;
         } else {
            BOpcUaCallbackHandler handler = (BOpcUaCallbackHandler)authnScheme.getAgentOn(BOpcUaCallbackHandler.class);
            if (handler != null) {
               handler.init(userName, password);
               SessionManager.addSession(this.opcUaSession);
               addSessionDataToHashMap(session.getSessionId(), this.opcUaSession.getId(), this.opcUaSession);
               BAuthenticationService authnService = (BAuthenticationService)Sys.getService(BAuthenticationService.TYPE);
               return authnService.authenticate(this.opcUaSession, requestedUser, handler, authnScheme);
            } else {
               this.logger.log(Level.SEVERE, lex.getText("opcUaUserAuth.noCallbackAgent", new Object[]{authnScheme.getSchemeName()}));
               return null;
            }
         }
      }
   }

   public void onValidationError(Session session, UserIdentityToken userToken, Exception exception) {
      String userIdentity = "Unknown User Identity";
      if (session != null && session.getUserIdentity() != null) {
         userIdentity = session.getUserIdentity().toString();
      }

      if (this.logger.isLoggable(Level.FINE)) {
         this.logger.log(Level.WARNING, lex.getText("opcUaUserValidation.error", new Object[]{userIdentity}), (Throwable)exception);
      } else {
         this.logger.log(Level.WARNING, lex.getText("opcUaUserValidation.error", new Object[]{userIdentity}) + ": " + exception.getLocalizedMessage());
      }
   }

   public static void invalidateSessionForNodeId(NodeId nodeId) {
      OpcUaUserValidator.SessionData existed = nodeIdSessionDataHashMap.remove(nodeId.toString());
      if (existed != null) {
         existed.getSession().invalidate();
      }
   }

   public static OpcUaUserValidator.SessionData getSessionDataFromHashMap(NodeId nodeId) {
      return nodeIdSessionDataHashMap.get(nodeId.toString());
   }

   public static void addSessionDataToHashMap(NodeId nodeId, String sessionId, BOpcUaServerSession session) {
      nodeIdSessionDataHashMap.put(nodeId.toString(), new OpcUaUserValidator.SessionData(sessionId, session));
   }

   public static class SessionData {
      String sessionId;
      BOpcUaServerSession session;

      public SessionData(String sessionId, BOpcUaServerSession session) {
         this.sessionId = sessionId;
         this.session = session;
      }

      public String getSessionId() {
         return this.sessionId;
      }

      public BOpcUaServerSession getSession() {
         return this.session;
      }
   }
}
