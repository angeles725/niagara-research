package com.tridium.basicdriver.util;

import com.tridium.basicdriver.BBasicNetwork;
import javax.baja.driver.util.BIPollable;
import javax.baja.driver.util.BPollScheduler;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.NotRunningException;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public class BBasicPollScheduler extends BPollScheduler {
   public static final Type TYPE = Sys.loadType(BBasicPollScheduler.class);

   public Type getType() {
      return TYPE;
   }

   public void doPoll(BIPollable p) throws Exception {
      boolean shouldPoll = true;

      try {
         BBasicNetwork net = (BBasicNetwork)this.getParent();
         shouldPoll = !net.isDisabled() && !net.isDown() && !net.isFault();
      } catch (Exception var7) {
         shouldPoll = true;
      }

      if (shouldPoll) {
         BIBasicPollable dev = (BIBasicPollable)p;

         try {
            if (((BBasicNetwork)this.getParent()).getLog().isTraceOn()) {
               ((BBasicNetwork)this.getParent()).getLog().trace("Poll <" + p + ">");
            }
         } catch (Exception var6) {
         }

         try {
            dev.poll();
         } catch (NotRunningException var5) {
            this.unsubscribe(dev);
         }
      }
   }
}
