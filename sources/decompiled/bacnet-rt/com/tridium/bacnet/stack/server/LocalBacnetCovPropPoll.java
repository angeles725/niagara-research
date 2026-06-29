package com.tridium.bacnet.stack.server;

import javax.baja.bacnet.datatypes.BBacnetCovSubscription;
import javax.baja.bacnet.export.BIBacnetCovSource;
import javax.baja.bacnet.export.BLocalBacnetDevice;
import javax.baja.bacnet.util.LocalBacnetPoll;
import javax.baja.sys.BObject;
import javax.baja.sys.BRelTime;
import javax.baja.sys.Type;

public class LocalBacnetCovPropPoll extends LocalBacnetPoll {
   private BLocalBacnetDevice local;

   public LocalBacnetCovPropPoll(BLocalBacnetDevice local) {
      this.local = local;
   }

   @Override
   protected BRelTime getPollRate() {
      return this.local.getCovPropertyPollRate();
   }

   @Override
   protected String getThreadName() {
      return "Local Bacnet COVProperty Poll";
   }

   @Override
   protected Type getPolledType() {
      return BBacnetCovSubscription.TYPE;
   }

   @Override
   protected boolean poll(BObject o) throws Exception {
      BBacnetCovSubscription sub = (BBacnetCovSubscription)o;
      BIBacnetCovSource covSrc = (BIBacnetCovSource)sub.getParent();
      if (covSrc == null) {
         return false;
      } else {
         covSrc.checkCov();
         return true;
      }
   }
}
