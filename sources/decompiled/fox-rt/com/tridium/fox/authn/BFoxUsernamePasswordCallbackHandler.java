package com.tridium.fox.authn;

import com.tridium.fox.message.FoxMessage;
import java.io.IOException;
import java.util.logging.Level;
import javax.baja.authn.AuthenticationUtil;
import javax.baja.fox.authn.BFoxCallbackHandler;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;

@NiagaraType
public class BFoxUsernamePasswordCallbackHandler extends BFoxCallbackHandler {
   public static final Type TYPE = Sys.loadType(BFoxUsernamePasswordCallbackHandler.class);
   String username = null;

   @Override
   public Type getType() {
      return TYPE;
   }

   public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
      FoxMessage login = null;

      for (Callback callback : callbacks) {
         if (callback instanceof NameCallback) {
            try {
               if (login == null) {
                  login = this.getLogin();
               }

               this.username = login.getString("username", null);
               ((NameCallback)callback).setName(this.username);
            } catch (Exception var8) {
               AuthenticationUtil.debug(Level.SEVERE, "Could not acquire username from NameCallback", var8);
               throw new IOException("Could not acquire username.");
            }
         } else {
            if (!(callback instanceof PasswordCallback)) {
               throw new UnsupportedCallbackException(callback, "Callback " + callback.getClass().getName() + " is not supported.");
            }

            try {
               if (login == null) {
                  login = this.getLogin();
               }

               String password = login.getString("password", null);
               ((PasswordCallback)callback).setPassword(password.toCharArray());
            } catch (Exception var9) {
               AuthenticationUtil.debug(Level.SEVERE, "Could not acquire password from PasswordCallback", var9);
               throw new IOException("Could not acquire password.");
            }
         }
      }
   }

   private FoxMessage getLogin() throws Exception {
      this.session.setState("FoxBasicCallbackHandler receive login");
      return this.session.receiveTuning("login");
   }

   @Override
   public String getUsername() {
      return this.username;
   }
}
