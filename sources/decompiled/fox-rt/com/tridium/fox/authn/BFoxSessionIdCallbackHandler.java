package com.tridium.fox.authn;

import com.tridium.authn.SessionIdCallback;
import com.tridium.fox.message.FoxMessage;
import java.io.IOException;
import java.util.logging.Level;
import javax.baja.authn.AuthenticationUtil;
import javax.baja.fox.authn.BFoxCallbackHandler;
import javax.baja.nre.annotations.AgentOn;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.UnsupportedCallbackException;

@NiagaraType(
   agent = {@AgentOn(
      types = {"baja:SessionIdAuthenticationScheme"}
   )}
)
public class BFoxSessionIdCallbackHandler extends BFoxCallbackHandler {
   public static final Type TYPE = Sys.loadType(BFoxSessionIdCallbackHandler.class);
   private String username;

   @Override
   public Type getType() {
      return TYPE;
   }

   @Override
   public String getUsername() {
      return this.username;
   }

   public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
      for (Callback callback : callbacks) {
         if (!(callback instanceof SessionIdCallback)) {
            throw new UnsupportedCallbackException(callback, "Callback " + callback.getClass().getName() + " is not supported.");
         }

         try {
            this.session.setState("FoxBasicCallbackHandler receive login");
            FoxMessage login = this.session.receiveTuning("login");
            ((SessionIdCallback)callback).setSessionId(login.getString("requestedSessionId"));
            this.username = login.getString("username");
            ((SessionIdCallback)callback).setUsername(this.username);
            ((SessionIdCallback)callback).setSession(this.session);
         } catch (Exception var7) {
            AuthenticationUtil.debug(Level.SEVERE, "Could not acquire username from NameCallback", var7);
            throw new IOException("Could not acquire username.");
         }
      }
   }
}
