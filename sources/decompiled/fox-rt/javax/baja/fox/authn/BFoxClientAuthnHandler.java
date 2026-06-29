package javax.baja.fox.authn;

import com.tridium.authn.AuthenticationClient;
import com.tridium.fox.session.FoxSession;
import javax.baja.agent.BIAgent;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BIObject;
import javax.baja.sys.BStruct;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public abstract class BFoxClientAuthnHandler extends BStruct implements BIAgent {
   public static final Type TYPE = Sys.loadType(BFoxClientAuthnHandler.class);

   public Type getType() {
      return TYPE;
   }

   public abstract void handleAuthentication(FoxSession var1, AuthenticationClient var2) throws Exception;

   public void setData(BIObject data) {
      throw new UnsupportedOperationException("Data type not supported.");
   }

   public void success(FoxSession session) {
   }

   public void failure(FoxSession session) {
   }
}
