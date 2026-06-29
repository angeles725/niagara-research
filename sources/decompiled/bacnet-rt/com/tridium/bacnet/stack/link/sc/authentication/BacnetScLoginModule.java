package com.tridium.bacnet.stack.link.sc.authentication;

import com.tridium.authn.NiagaraFailedLoginException;
import com.tridium.authn.NiagaraLoginModule;
import java.util.logging.Level;
import javax.baja.authn.AuthenticationUtil;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;

public final class BacnetScLoginModule extends NiagaraLoginModule {
   public boolean login() throws LoginException {
      if (this.callbackHandler == null) {
         throw new LoginException("CallbackHandler cannot be null for BACnet/SC login");
      } else {
         Callback[] callbacks = new Callback[]{new BacnetScCallback()};

         String username;
         try {
            this.callbackHandler.handle(callbacks);
            username = ((BacnetScCallback)callbacks[0]).getUsername();
         } catch (UnsupportedCallbackException var6) {
            String msg = "Provided callback handler could not handle: " + var6.getCallback().toString();
            AuthenticationUtil.debug(Level.SEVERE, msg, var6);
            throw new LoginException(msg);
         } catch (Exception var7) {
            String msgx = "Error handling callbacks:" + var7;
            AuthenticationUtil.debug(Level.SEVERE, msgx, var7);
            throw new LoginException(msgx);
         }

         if (username != null) {
            try {
               this.user = this.getUserService().getUser(username);
               if (this.user != null && this.getUserService().canLogin(this.user)) {
                  this.succeeded = true;
                  return true;
               }
            } catch (Exception var5) {
            }
         }

         FailedLoginException e;
         if (this.user != null) {
            e = new NiagaraFailedLoginException(this.user.getName(), "Login failed: Invalid username or password.");
            this.user = null;
         } else {
            e = new FailedLoginException("Login failed: Invalid username or password.");
         }

         throw e;
      }
   }
}
