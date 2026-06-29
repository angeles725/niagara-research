package com.tridium.fox.authn;

import com.tridium.authn.BDigestAuthenticationScheme;
import com.tridium.authn.BLegacyDigestAuthenticationScheme;
import com.tridium.authn.ScramServerCallback;
import com.tridium.fox.util.FoxScramShaUtil;
import java.io.IOException;
import javax.baja.fox.authn.BFoxCallbackHandler;
import javax.baja.nre.annotations.AgentOn;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.UnsupportedCallbackException;

@NiagaraType(
   agent = {@AgentOn(
      types = {"baja:DigestAuthenticationScheme", "baja:LegacyDigestAuthenticationScheme"}
   )}
)
public class BFoxDigestCallbackHandler extends BFoxCallbackHandler {
   public static final Type TYPE = Sys.loadType(BFoxDigestCallbackHandler.class);
   String username = null;

   @Override
   public Type getType() {
      return TYPE;
   }

   public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
      for (Callback callback : callbacks) {
         if (!(callback instanceof ScramServerCallback)) {
            throw new UnsupportedCallbackException(callback, "Callback " + callback.getClass().getName() + " is not supported.");
         }

         Type schemeType = this.session.isLegacyConnection() ? BLegacyDigestAuthenticationScheme.TYPE : BDigestAuthenticationScheme.TYPE;
         this.username = FoxScramShaUtil.handleScramServerCallback(this.session, (ScramServerCallback)callback, schemeType);
      }
   }

   @Override
   public String getUsername() {
      return this.username;
   }
}
