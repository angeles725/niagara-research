package com.tridium.lonworks.local;

import com.tridium.lonworks.device.NvDev;
import com.tridium.lonworks.util.NmUtil;
import javax.baja.lonworks.BINetworkVariable;
import javax.baja.lonworks.BINvContainer;
import javax.baja.lonworks.BLonDevice;
import javax.baja.lonworks.BLonLink;
import javax.baja.lonworks.BLonNetwork;
import javax.baja.lonworks.datatypes.BDeviceData;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.status.BStatus;
import javax.baja.sys.BComponent;
import javax.baja.sys.BValue;
import javax.baja.sys.Context;
import javax.baja.sys.Knob;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "status",
      type = "BStatus",
      defaultValue = "BStatus.ok",
      flags = 75
   ), @NiagaraProperty(
      name = "faultCause",
      type = "String",
      defaultValue = "",
      flags = 67
   )})
public class BPseudoNvContainer extends BComponent implements BINvContainer {
   public static final Property status = newProperty(75, BStatus.ok, null);
   public static final Property faultCause = newProperty(67, "", null);
   public static final Type TYPE = Sys.loadType(BPseudoNvContainer.class);
   boolean linkUpdateDone = false;
   NvDev.SaveNv snv = null;

   public BStatus getStatus() {
      return (BStatus)this.get(status);
   }

   public void setStatus(BStatus v) {
      this.set(status, v, null);
   }

   public String getFaultCause() {
      return this.getString(faultCause);
   }

   public void setFaultCause(String v) {
      this.setString(faultCause, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public void started() throws Exception {
      super.started();
      this.checkStatus();
   }

   private void checkStatus() {
      if (this.getLonNetwork() == null) {
         this.setStatus(BStatus.fault);
         this.setFaultCause("No LonNetwork in parentage.");
      } else {
         this.setStatus(BStatus.ok);
         this.setFaultCause("");
      }
   }

   @Override
   public BINetworkVariable[] getNetworkVariables() {
      return null;
   }

   @Override
   public BDeviceData getDeviceData() {
      return this.getLonDevice().getDeviceData();
   }

   @Override
   public BLonNetwork getLonNetwork() {
      return NmUtil.getLonNetwork(this);
   }

   @Override
   public BLonDevice getLonDevice() {
      return this.getLonNetwork().getLocalLonDevice();
   }

   @Override
   public boolean isLonObject() {
      return false;
   }

   public boolean isOk() {
      return this.getStatus().isOk();
   }

   @Override
   public void linkUpdate() {
      if (!this.linkUpdateDone) {
         this.getComponentSpace().update(this, 1);
         this.getComponentSpace().update(this.getLonDevice(), 1);
         this.getComponentSpace().update(this.getLonDevice().getDeviceData(), 2);
         this.linkUpdateDone = true;
      }
   }

   public void added(Property prop, Context context) {
      super.added(prop, context);
      if (prop.getType().is(BLonLink.TYPE)) {
         BLonLink lnk = (BLonLink)this.get(prop);
         if (this.isRunning() && !lnk.getMessageTag()) {
            lnk.getDestinationNv().lonLinkAdded();
         }
      }
   }

   public void checkRemove(Property prop, Context context) {
      this.snv = NvDev.checkRemove(this, prop, context);
      super.checkRemove(prop, context);
   }

   public void removed(Property prop, BValue value, Context context) {
      super.removed(prop, value, context);
      NvDev.removed(this, this.snv, prop, value, context);
      this.snv = null;
   }

   public final void knobAdded(Knob knob, Context context) {
      NvDev.knobAdded(this, knob, context);
   }

   public final void knobRemoved(Knob knob, Context context) {
      NvDev.knobRemoved(this, knob, context);
   }
}
