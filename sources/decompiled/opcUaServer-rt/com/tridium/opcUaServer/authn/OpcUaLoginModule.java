package com.tridium.opcUaServer.authn;

import com.tridium.authn.NiagaraFailedLoginException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.authn.AuthenticationUtil;
import javax.baja.security.BPasswordCache;
import javax.baja.sys.Sys;
import javax.baja.user.BUser;
import javax.baja.user.BUserService;
import javax.baja.util.Lexicon;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

public class OpcUaLoginModule implements LoginModule {
   public static final Logger logger = Logger.getLogger("opcua.auth");
   private static BUserService userService = (BUserService)Sys.getService(BUserService.TYPE);
   private static final Lexicon lex = Lexicon.make(OpcUaLoginModule.class);
   private Subject subject;
   private CallbackHandler callbackHandler;
   private boolean succeeded = false;
   private boolean commitSucceeded = false;
   private BUser user = null;

   @Override
   public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState, Map<String, ?> options) {
      this.subject = subject;
      this.callbackHandler = callbackHandler;
   }

   @Override
   public boolean login() throws LoginException {
      if (this.callbackHandler == null) {
         throw new LoginException(lex.getText("opcUaAuth.callbackHandler"));
      } else {
         Callback[] callbacks = new Callback[]{new NameCallback("name"), new PasswordCallback("password", false)};

         String username;
         char[] password;
         try {
            this.callbackHandler.handle(callbacks);
            username = ((NameCallback)callbacks[0]).getName();
            if (username.isEmpty()) {
               return false;
            }

            char[] tempPassword = ((PasswordCallback)callbacks[1]).getPassword();
            password = new char[tempPassword.length];
            System.arraycopy(tempPassword, 0, password, 0, tempPassword.length);
            ((PasswordCallback)callbacks[1]).clearPassword();
         } catch (UnsupportedCallbackException var6) {
            String msg = lex.getText("opcuaAuth.unsupportedCallback") + var6.getCallback().toString();
            AuthenticationUtil.debug(Level.SEVERE, msg, var6);
            throw new LoginException(msg);
         } catch (Exception var7) {
            String msgx = lex.getText("opcUaAuth.callbackError") + var7.toString();
            AuthenticationUtil.debug(Level.SEVERE, msgx, var7);
            throw new LoginException(msgx);
         }

         this.user = userService.getUser(username);
         if (this.user == null
            || !(this.user.getAuthenticator() instanceof BPasswordCache)
            || !((BPasswordCache)this.user.getAuthenticator()).validate(new String(password))) {
            for (int i = 0; i < password.length; i++) {
               password[i] = 0;
            }

            password = null;
            this.succeeded = false;
            throw new NiagaraFailedLoginException(lex.getText("opcUaAuth.loginFail"));
         } else if (!userService.canLogin(this.user)) {
            logger.severe(lex.getText("opcuaAuth.userStatus") + this.user.getStatus());
            return false;
         } else {
            this.succeeded = true;
            return true;
         }
      }
   }

   @Override
   public boolean commit() throws LoginException {
      return !this.succeeded ? false : AccessController.doPrivileged((PrivilegedAction<Boolean>)(() -> {
         if (!this.subject.getPrincipals().contains(this.user)) {
            this.subject.getPrincipals().add(this.user);
         }

         this.commitSucceeded = true;
         return true;
      }));
   }

   @Override
   public boolean abort() throws LoginException {
      if (!this.succeeded) {
         return false;
      } else {
         if (!this.commitSucceeded) {
            this.succeeded = false;
         } else {
            this.logout();
         }

         return true;
      }
   }

   @Override
   public boolean logout() throws LoginException {
      return AccessController.doPrivileged((PrivilegedAction<Boolean>)(() -> {
         if (this.user != null) {
            this.subject.getPrincipals().remove(this.user);
         }

         this.succeeded = false;
         this.commitSucceeded = false;
         this.user = null;
         return true;
      }));
   }
}
