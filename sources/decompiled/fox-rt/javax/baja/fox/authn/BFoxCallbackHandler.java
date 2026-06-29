package javax.baja.fox.authn;

import com.tridium.authn.BCallbackHandler;
import com.tridium.fox.session.FoxSession;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public abstract class BFoxCallbackHandler extends BCallbackHandler {
   public static final Type TYPE = Sys.loadType(BFoxCallbackHandler.class);
   protected FoxSession session;

   public Type getType() {
      return TYPE;
   }

   public void init(FoxSession session) {
      this.session = session;
   }

   public abstract String getUsername();
}
