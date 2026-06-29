package com.tridium.fox.authn;

import com.tridium.authn.AuthenticationClient;
import com.tridium.fox.message.FoxMessage;
import com.tridium.fox.session.FoxSession;
import javax.baja.fox.authn.BFoxClientAuthnHandler;
import javax.baja.nre.annotations.AgentOn;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.security.BHttpFoxCredentials;
import javax.baja.security.BUsernameCredential;
import javax.baja.sys.BIObject;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType(
   agent = {@AgentOn(
      types = {"baja:SessionIdAuthenticationScheme"}
   )}
)
public class BFoxSessionIdClientAuthnHandler extends BFoxClientAuthnHandler {
   public static final Type TYPE = Sys.loadType(BFoxSessionIdClientAuthnHandler.class);
   String requestedSessionId;
   String username;

   @Override
   public Type getType() {
      return TYPE;
   }

   @Override
   public void handleAuthentication(FoxSession session, AuthenticationClient authenticationClient) throws Exception {
      FoxMessage login = new FoxMessage();
      login.add("username", this.username);
      login.add("requestedSessionId", this.requestedSessionId);
      session.setState("client.tune sendLogin");
      session.sendTuning("login", login);
   }

   @Override
   public void setData(BIObject data) {
      if (data instanceof BHttpFoxCredentials) {
         this.requestedSessionId = ((BHttpFoxCredentials)data).getSessionId();
      } else {
         if (!(data instanceof BUsernameCredential)) {
            throw new UnsupportedOperationException("Data type not supported.");
         }

         this.username = ((BUsernameCredential)data).getUsername();
      }
   }

   @Override
   public void success(FoxSession session) {
      session.setRemoteSuperId(this.requestedSessionId);
   }
}
