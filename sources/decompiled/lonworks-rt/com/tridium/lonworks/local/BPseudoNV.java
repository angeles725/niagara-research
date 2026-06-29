package com.tridium.lonworks.local;

import com.tridium.lonworks.device.DynaDev;
import com.tridium.lonworks.util.NmUtil;
import java.util.logging.Level;
import javax.baja.driver.point.BTuningPolicy;
import javax.baja.lonworks.BLonDevice;
import javax.baja.lonworks.BLonLink;
import javax.baja.lonworks.BLonNetwork;
import javax.baja.lonworks.BNetworkVariable;
import javax.baja.lonworks.datatypes.BImplicit;
import javax.baja.lonworks.datatypes.BNvConfigData;
import javax.baja.lonworks.datatypes.BNvProps;
import javax.baja.lonworks.enums.BLonNvDirection;
import javax.baja.lonworks.londata.BLonData;
import javax.baja.lonworks.util.SnvtUtil;
import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.util.Array;
import javax.baja.status.BStatus;
import javax.baja.sys.BComponent;
import javax.baja.sys.BajaRuntimeException;
import javax.baja.sys.Context;
import javax.baja.sys.Knob;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "nvConfigData",
      type = "BNvConfigData",
      defaultValue = "new BNvConfigData()",
      flags = 1,
      override = true
   ), @NiagaraProperty(
      name = "nvProps",
      type = "BNvProps",
      defaultValue = "new BNvProps()",
      flags = 1,
      override = true
   ), @NiagaraProperty(
      name = "tuningPolicyName",
      type = "String",
      defaultValue = "defaultPolicy",
      flags = 7,
      facets = {@Facet("TUNING_POLICY_NAME_FACETS")},
      override = true
   )})
public class BPseudoNV extends BNetworkVariable {
   public static final Property nvConfigData = newProperty(1, new BNvConfigData(), null);
   public static final Property nvProps = newProperty(1, new BNvProps(), null);
   public static final Property tuningPolicyName = newProperty(7, "defaultPolicy", TUNING_POLICY_NAME_FACETS);
   public static final Type TYPE = Sys.loadType(BPseudoNV.class);

   @Override
   public Type getType() {
      return TYPE;
   }

   public BPseudoNV() {
   }

   public BPseudoNV(int snvtType, BLonNvDirection direction) {
      this.getNvProps().setSnvtType(snvtType);
      this.getNvConfigData().setDirection(direction);
      BLonData dat = SnvtUtil.getLonData(snvtType);
      DynaDev.setNonCritical(dat);
      this.setData(dat);
   }

   @Override
   public void setData(BLonData v) {
      if (v != null) {
         Property[] a = this.getPropertiesArray();

         for (int i = 0; i < a.length; i++) {
            if (isDataProp(a[i])) {
               this.remove(a[i]);
            }
         }

         a = v.getPropertiesArray();

         for (int ix = 0; ix < a.length; ix++) {
            Property prop = a[ix];
            if (isDataProp(prop)) {
               this.add(prop.getName(), v.get(prop), v.getFlags(prop), prop.getFacets(), null);
            }
         }
      }
   }

   @Override
   public void lonComponentStarted() {
      if (this.getPseudoNvContainer().isOk()) {
         if (this.getNvConfigData().getSelector() == -1) {
            this.getNvConfigData().setUnbound(this.getNvProps().getNvIndex());
         }

         this.getTuning().transition();
         this.evaluateLinkedState();
      }
   }

   @Override
   public void lonComponentStopped() {
      if (this.getPseudoNvContainer().isOk()) {
         this.getTuning().transition();
      }
   }

   @Override
   public void atSteadyState() {
      if (this.getPseudoNvContainer().isOk()) {
         this.getTuning().transition();
      }
   }

   @Override
   public boolean isWriteable() {
      return this.getNvConfigData().isOutput();
   }

   @Override
   public void subscribed() {
   }

   @Override
   public void unsubscribed() {
   }

   @Override
   public void doForceRead() {
   }

   @Override
   protected void dataChanged(Context cx) {
      if (this.isRunning()) {
         if (cx == null || !BLonNetwork.lonNoPropagate.equals(cx)) {
            this.propagateLinks();
         }
      }
   }

   @Override
   public boolean write(Context cx) {
      if (!this.getPseudoNvContainer().isOk()) {
         return false;
      } else {
         boolean stateWrite = cx == BTuningPolicy.maxWriteTimeContext
            || cx == BTuningPolicy.writeOnStartContext
            || cx == BTuningPolicy.writeOnUpContext
            || cx == BTuningPolicy.writeOnEnabledContext;
         if (!stateWrite || this.isLinked() && this.getNvConfigData().isBoundNv()) {
            this.forceWrite();
            return false;
         } else {
            this.getTuning().writeOk();
            return false;
         }
      }
   }

   @Override
   public void doForceWrite() {
      if (this.getPseudoNvContainer().isOk()) {
         BNvConfigData configData = this.getNvConfigData();

         try {
            if (configData.isOutput() && configData.getAddrIndex() != -1) {
               NmUtil.setNvValue(
                  BImplicit.make(configData.getAddrIndex()),
                  NmUtil.getLonNetwork(this).lonComm(),
                  configData.getDirection().reverse(),
                  configData.getSelector(),
                  configData.getServiceType(),
                  configData.getAuthenticated(),
                  this.getData().toNetBytes()
               );
               this.getData().writeOk();
               this.getTuning().writeOk();
            }
         } catch (Throwable var4) {
            this.getData().writeFail(var4.toString());
            this.getTuning().writeFail();
            String errMsg = "Unable to write " + this.debugName();
            this.lonNetwork().log().log(Level.SEVERE, errMsg, var4);
            throw new BajaRuntimeException(errMsg + " " + var4.getMessage(), var4);
         }
      }
   }

   @Override
   public boolean isFatalFault() {
      return false;
   }

   @Override
   public BStatus getStatus() {
      return BStatus.ok;
   }

   @Override
   public boolean isParentLegal(BComponent parent) {
      return parent instanceof BPseudoNvContainer;
   }

   public BPseudoNvContainer getPseudoNvContainer() {
      return (BPseudoNvContainer)this.getParent();
   }

   @Override
   public void lonKnobAdded(Knob knob) {
      Array<BLonLink> a = this.getLinksToPropagate(true);
      synchronized (a) {
         a.add((BLonLink)knob.getLink());
      }
   }

   @Override
   public void lonKnobRemove(Knob knob) {
      Array<BLonLink> a = this.getLinksToPropagate(false);
      if (a != null) {
         synchronized (a) {
            a.remove((BLonLink)knob.getLink());
         }
      }
   }

   @Override
   public BLonDevice getDevice() {
      return this.lonNetwork().getLocalLonDevice();
   }
}
