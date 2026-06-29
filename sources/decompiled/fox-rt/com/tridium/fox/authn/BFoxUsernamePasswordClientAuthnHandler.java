package com.tridium.fox.authn;

import com.tridium.authn.AuthenticationClient;
import com.tridium.fox.message.FoxMessage;
import com.tridium.fox.session.FoxAuthenticationException;
import com.tridium.fox.session.FoxSecureRequiredException;
import com.tridium.fox.session.FoxSession;
import com.tridium.fox.sys.BFoxClientConnection;
import java.security.AccessController;
import javax.baja.fox.authn.BFoxClientAuthnHandler;
import javax.baja.nre.annotations.AgentOn;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.security.AuthenticationRealm;
import javax.baja.security.BICredentials;
import javax.baja.security.BPassword;
import javax.baja.security.BUsernameAndPassword;
import javax.baja.security.BUsernameCredential;
import javax.baja.sys.BIObject;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.util.Lexicon;

@NiagaraType(
   agent = {@AgentOn(
      types = {"baja:LegacyBasicAuthenticationScheme"}
   )}
)
public class BFoxUsernamePasswordClientAuthnHandler extends BFoxClientAuthnHandler {
   public static final Type TYPE = Sys.loadType(BFoxUsernamePasswordClientAuthnHandler.class);
   private static final int USERNAME_PASSWORD_STEP = 0;
   private BUsernameCredential username = new BUsernameCredential();
   private BICredentials credentials = null;

   @Override
   public Type getType() {
      return TYPE;
   }

   @Override
   public void handleAuthentication(FoxSession session, AuthenticationClient authenticationClient) throws Exception {
      BFoxClientConnection foxClientConnection = (BFoxClientConnection)session.conn();
      AuthenticationRealm realm = foxClientConnection.getFoxSession();
      BUsernameAndPassword seedCredential = new BUsernameAndPassword(this.username.getUsername(), BPassword.DEFAULT);
      BUsernameAndPassword cred = null;
      String scheme = foxClientConnection.getAuthenticationScheme();
      if (this.credentials == null) {
         this.credentials = authenticationClient.requestInformation(realm, scheme, 0, seedCredential);
      }

      if (this.credentials == null) {
         throw new FoxAuthenticationException("Cancelled", scheme, null, session);
      } else if (this.credentials instanceof BUsernameAndPassword) {
         cred = (BUsernameAndPassword)this.credentials;
         if (!foxClientConnection.getUseFoxs()) {
            throw new FoxSecureRequiredException(session);
         } else {
            FoxMessage login = new FoxMessage();
            login.add("username", cred.getUsername());
            login.add("password", AccessController.doPrivileged(cred.getPassword()::getValue));
            session.setState("client.tune sendLogin");
            session.sendTuning("login", login);
         }
      } else {
         throw new FoxAuthenticationException(
            Lexicon.make("fox").getText("usernamePassword.authnHandler.unsupportedCredentials", new Object[]{scheme, this.credentials.getType()}),
            scheme,
            null,
            session
         );
      }
   }

   @Override
   public void setData(BIObject data) {
      if (data instanceof BUsernameCredential) {
         this.username = (BUsernameCredential)data;
      } else {
         throw new UnsupportedOperationException("Data type not supported.");
      }
   }
}
