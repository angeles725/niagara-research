package com.tridium.fox.authn;

import com.tridium.authn.AuthenticationClient;
import com.tridium.fox.session.FoxAuthenticationException;
import com.tridium.fox.session.FoxSession;
import com.tridium.fox.sys.BFoxClientConnection;
import com.tridium.fox.util.FoxScramShaUtil;
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
      types = {"baja:DigestAuthenticationScheme", "baja:LegacyDigestAuthenticationScheme"}
   )}
)
public class BFoxDigestClientAuthnHandler extends BFoxClientAuthnHandler {
   public static final Type TYPE = Sys.loadType(BFoxDigestClientAuthnHandler.class);
   private BUsernameCredential username = new BUsernameCredential();

   @Override
   public Type getType() {
      return TYPE;
   }

   @Override
   public void handleAuthentication(FoxSession session, AuthenticationClient authenticationClient) throws Exception {
      BFoxClientConnection foxClientConnection = (BFoxClientConnection)session.conn();
      AuthenticationRealm realm = foxClientConnection.getFoxSession();
      String scheme = foxClientConnection.getAuthenticationScheme();
      BUsernameAndPassword seedCredential = new BUsernameAndPassword(this.username.getUsername(), BPassword.DEFAULT);
      BICredentials credentials = authenticationClient.requestInformation(realm, scheme, 0, seedCredential);
      if (credentials == null) {
         throw new FoxAuthenticationException("Cancelled", scheme, null, session);
      } else if (credentials instanceof BUsernameAndPassword) {
         BUsernameAndPassword cred = (BUsernameAndPassword)credentials;
         FoxScramShaUtil.handleClientAuthentication(session, cred, scheme);
      } else {
         throw new FoxAuthenticationException(
            Lexicon.make("fox").getText("digest.authnHandler.unsupportedCredentials", new Object[]{credentials.getType()}), scheme, null, session
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
