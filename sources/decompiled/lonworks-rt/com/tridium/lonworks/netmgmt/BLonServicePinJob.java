package com.tridium.lonworks.netmgmt;

import com.tridium.lonworks.BLonRouter;
import com.tridium.lonworks.NetMessageReceiver;
import com.tridium.lonworks.enums.BServicePinState;
import com.tridium.lonworks.netmessages.ServicePin;
import javax.baja.lonworks.BLonDevice;
import javax.baja.lonworks.BLonNetwork;
import javax.baja.lonworks.LonException;
import javax.baja.lonworks.datatypes.BNeuronId;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BComponent;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperty(
   name = "servicePinState",
   type = "BServicePinState",
   defaultValue = "BServicePinState.idle"
)
public abstract class BLonServicePinJob extends BLonNetmgmtJob {
   public static final Property servicePinState = newProperty(0, BServicePinState.idle, null);
   public static final Type TYPE = Sys.loadType(BLonServicePinJob.class);

   public BServicePinState getServicePinState() {
      return (BServicePinState)this.get(servicePinState);
   }

   public void setServicePinState(BServicePinState v) {
      this.set(servicePinState, v, null);
   }

   @Override
   public Type getType() {
      return TYPE;
   }

   public BLonServicePinJob() {
   }

   public BLonServicePinJob(BLonNetmgmt netMgmt) {
      super(netMgmt);
   }

   public ServicePin receiveServicePin() throws LonException {
      ServicePin srvpin = null;
      NetMessageReceiver rcv = this.netMgmt.lonNetwork().netMessageReceiver();
      rcv.clearServicePin();

      try {
         if (!this.isCanceling()) {
            srvpin = rcv.getServicePin(this.netMgmt.getServicePinWait() * 1000);
         }
      } catch (InterruptedException var4) {
      }

      if (srvpin == null) {
         if (this.isCanceling()) {
            this.canceled();
         } else {
            this.fatal("timed out waiting for service pin");
         }
      }

      return srvpin;
   }

   public void validateNeuronId(BNeuronId nid, BComponent spDev) throws LonException {
      BLonNetwork lon = this.netMgmt.lonNetwork();
      BLonDevice dev = lon.findDevice(nid);
      if (dev != null && dev != spDev) {
         throw new LonException("NeuronId already in use by " + dev.getDisplayName(null));
      } else {
         BLonRouter rtr = lon.findRouter(nid);
         if (rtr != null && rtr != spDev) {
            throw new LonException("NeuronId already in use by " + rtr.getDisplayName(null));
         }
      }
   }

   @Override
   public void doCancel(Context cx) {
      super.doCancel(cx);
      this.netMgmt.lonNetwork().netMessageReceiver().cancelServicePin();
   }
}
