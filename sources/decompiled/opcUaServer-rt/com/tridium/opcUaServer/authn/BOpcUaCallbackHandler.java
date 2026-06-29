package com.tridium.opcUaServer.authn;

import com.tridium.authn.BCallbackHandler;
import java.io.IOException;
import javax.baja.nre.annotations.AgentOn;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;

@NiagaraType(
   agent = {@AgentOn(
      types = {"opcUaServer:OpcUaAuthenticationScheme"}
   )}
)
public class BOpcUaCallbackHandler extends BCallbackHandler {
   public static final Type TYPE = Sys.loadType(BOpcUaCallbackHandler.class);
   private String userName;
   private String password;

   public Type getType() {
      return TYPE;
   }

   public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
      for (Callback callback : callbacks) {
         if (callback instanceof NameCallback) {
            ((NameCallback)callback).setName(this.userName);
         } else {
            if (!(callback instanceof PasswordCallback)) {
               throw new UnsupportedCallbackException(callback);
            }

            ((PasswordCallback)callback).setPassword(this.password.toCharArray());
         }
      }
   }

   public void init(String userName, String password) {
      this.userName = userName;
      this.password = password;
   }
}
