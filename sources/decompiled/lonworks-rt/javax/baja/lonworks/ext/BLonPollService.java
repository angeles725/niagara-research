package javax.baja.lonworks.ext;

import javax.baja.driver.util.BIPollable;
import javax.baja.driver.util.BPollScheduler;
import javax.baja.lonworks.BLonNetwork;
import javax.baja.lonworks.BNetworkVariable;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public class BLonPollService extends BPollScheduler {
   public static final Type TYPE = Sys.loadType(BLonPollService.class);

   public Type getType() {
      return TYPE;
   }

   public void doPoll(BIPollable p) throws Exception {
      if (((BLonNetwork)this.getParent()).isServiceRunning()) {
         try {
            ((BNetworkVariable)p).pollNv();
         } catch (Throwable var3) {
         }
      }
   }
}
