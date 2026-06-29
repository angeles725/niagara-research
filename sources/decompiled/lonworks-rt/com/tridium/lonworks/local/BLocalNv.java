package com.tridium.lonworks.local;

import com.tridium.lonworks.device.DynaDev;
import com.tridium.lonworks.util.NmUtil;
import java.util.logging.Level;
import javax.baja.driver.point.BITunable;
import javax.baja.driver.point.BReadWriteMode;
import javax.baja.driver.point.Tuning;
import javax.baja.lonworks.BINetworkVariable;
import javax.baja.lonworks.BLocalLonDevice;
import javax.baja.lonworks.BLonComponent;
import javax.baja.lonworks.BNetworkVariable;
import javax.baja.lonworks.datatypes.BNvConfigData;
import javax.baja.lonworks.datatypes.BNvProps;
import javax.baja.lonworks.enums.BLonNvDirection;
import javax.baja.lonworks.enums.BLonSnvtType;
import javax.baja.lonworks.londata.BLonData;
import javax.baja.lonworks.util.SnvtUtil;
import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraActions;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.status.BStatus;
import javax.baja.sys.Action;
import javax.baja.sys.BComponent;
import javax.baja.sys.BIcon;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "nvProps",
      type = "BNvProps",
      defaultValue = "new BNvProps()"
   ), @NiagaraProperty(
      name = "nvConfigData",
      type = "BNvConfigData",
      defaultValue = "new BNvConfigData()"
   ), @NiagaraProperty(
      name = "selfDoc",
      type = "String",
      defaultValue = ""
   ), @NiagaraProperty(
      name = "tuningPolicyName",
      type = "String",
      defaultValue = "defaultPolicy",
      facets = {@Facet("TUNING_POLICY_NAME_FACETS")}
   )})
@NiagaraActions({@NiagaraAction(
      name = "forceRead",
      flags = 20,
      override = true
   ), @NiagaraAction(
      name = "forceWrite",
      flags = 20,
      override = true
   )})
public class BLocalNv extends BLonComponent implements BINetworkVariable, BITunable {
   public static final Property nvProps = newProperty(0, new BNvProps(), null);
   public static final Property nvConfigData = newProperty(0, new BNvConfigData(), null);
   public static final Property selfDoc = newProperty(0, "", null);
   public static final Property tuningPolicyName = newProperty(0, "defaultPolicy", TUNING_POLICY_NAME_FACETS);
   public static final Action forceRead = newAction(20, null);
   public static final Action forceWrite = newAction(20, null);
   public static final Type TYPE = Sys.loadType(BLocalNv.class);
   private static final BIcon icon = BIcon.make("module://lonworks/com/tridium/lonworks/ui/icons/nv.png");
   private static int UNREGISTORED_SELECTOR = -1;
   private int registeredSelector = UNREGISTORED_SELECTOR;
   Tuning tuning = new Tuning(this);

   public BNvProps getNvProps() {
      return (BNvProps)this.get(nvProps);
   }

   public void setNvProps(BNvProps v) {
      this.set(nvProps, v, null);
   }

   @Override
   public BNvConfigData getNvConfigData() {
      return (BNvConfigData)this.get(nvConfigData);
   }

   @Override
   public void setNvConfigData(BNvConfigData v) {
      this.set(nvConfigData, v, null);
   }

   public String getSelfDoc() {
      return this.getString(selfDoc);
   }

   public void setSelfDoc(String v) {
      this.setString(selfDoc, v, null);
   }

   public String getTuningPolicyName() {
      return this.getString(tuningPolicyName);
   }

   public void setTuningPolicyName(String v) {
      this.setString(tuningPolicyName, v, null);
   }

   @Override
   public Type getType() {
      return TYPE;
   }

   public BLocalNv() {
   }

   public BLocalNv(int nvIndex, int snvtType, int objectIndex, int memberIndex, int flags, BLonNvDirection direction, String sdoc) {
      BNvProps nvProps = this.getNvProps();
      nvProps.setNvIndex(nvIndex);
      nvProps.setSnvtType(snvtType);
      nvProps.setObjectIndex(objectIndex);
      nvProps.setMemberIndex(memberIndex);
      BNvConfigData nvCfg = this.getNvConfigData();
      nvCfg.setDirection(direction);
      BNetworkVariable.setFlags(nvProps, nvCfg, flags);
      BLonData dat = SnvtUtil.getLonData(snvtType);
      DynaDev.setNonCritical(dat);
      this.setData(dat);
      this.setSelfDoc(sdoc);
   }

   public BIcon getIcon() {
      return icon;
   }

   @Override
   public boolean isParentLegal(BComponent parent) {
      return parent instanceof BLocalLonDevice;
   }

   @Override
   public final void lonComponentStarted() {
      this.getTuning().transition();
      this.registerSelector();
   }

   @Override
   public final void lonComponentStopped() {
      this.getTuning().transition();
      this.unregisterSelector();
   }

   public void atSteadyState() throws Exception {
      super.atSteadyState();
      this.getTuning().transition();
   }

   @Override
   public void changed(Property prop, Context context) {
      super.changed(prop, context);
      if (this.isRunning() && context != BLocalLonDevice.noInfoChange) {
         if (prop == nvProps || prop == selfDoc) {
            this.nvChanged();
         } else if (prop == nvConfigData) {
            this.registerSelector();
         }
      }
   }

   @Override
   protected void dataChanged(Context context) {
      if (this.isRunning() && context != BLocalLonDevice.noInfoChange) {
         BNvConfigData nvCfg = this.getNvConfigData();
         if (nvCfg.isOutput()) {
            this.getTuning().writeDesired();
         }
      }
   }

   @Override
   public void doForceWrite() {
      BNvConfigData nvCfg = this.getNvConfigData();
      if (nvCfg.isOutput()) {
         try {
            if (nvCfg.isBoundNv()) {
               NmUtil.sendNvUpdate(this.local(), nvCfg, this.getData().toNetBytes());
            }

            this.getData().writeOk();
            this.getTuning().writeOk();
         } catch (Throwable var3) {
            this.getData().writeFail(var3.toString());
            this.getTuning().writeFail();
            this.lonNetwork().log().log(Level.SEVERE, "Unable to write " + this.getParent().getDisplayName(null) + ":" + this.getDisplayName(null), var3);
         }
      }
   }

   private void nvChanged() {
      this.local().nvChanged(this);
   }

   @Override
   public void doForceRead() {
   }

   @Override
   public boolean isWriteable() {
      return this.getNvConfigData().isOutput();
   }

   private BLocalLonDevice local() {
      return (BLocalLonDevice)this.lonDevice();
   }

   @Override
   public int getNvIndex() {
      return this.getNvProps().getNvIndex();
   }

   @Override
   public void setNvIndex(int nvIndex) {
      this.getNvProps().setNvIndex(nvIndex);
   }

   @Override
   public int getSnvtType() {
      return this.getNvProps().getSnvtType();
   }

   @Override
   public void setUnbound() {
      this.getNvConfigData().setUnbound(this.getNvProps().getNvIndex());
      this.getNvProps().setUnbound();
   }

   @Override
   public boolean isLocalNv() {
      return true;
   }

   @Override
   public void receiveUpdate(byte[] nvData) {
      try {
         this.getData().fromNetBytes(nvData);
         this.getData().readOk();
         this.getTuning().readOk();
      } catch (Throwable var3) {
         this.getData().readFail(var3.toString());
         this.getTuning().readFail();
         this.lonNetwork()
            .log()
            .log(Level.SEVERE, "Could not decode nv update data " + this.getParent().getDisplayName(null) + ":" + this.getDisplayName(null), var3);
      }
   }

   public void configure(BLonSnvtType type, BLonNvDirection dir, String sDoc) {
      BNvProps nvProps = this.getNvProps();
      int snvtType = type.getOrdinal();
      nvProps.setInt(BNvProps.snvtType, snvtType, BLocalLonDevice.noInfoChange);
      if (sDoc != null) {
         this.setString(selfDoc, sDoc, BLocalLonDevice.noInfoChange);
      }

      this.getNvConfigData().set(BNvConfigData.direction, dir, BLocalLonDevice.noInfoChange);
      BLonData dat = SnvtUtil.getLonData(snvtType, 2);
      DynaDev.setNonCritical(dat);
      this.setData(dat);
   }

   private void registerSelector() {
      if (!this.getNvConfigData().isOutput()) {
         int newSel = this.getNvConfigData().getSelector();
         if (this.registeredSelector != newSel) {
            if (this.registeredSelector != UNREGISTORED_SELECTOR) {
               this.lonNetwork().nvManager().unregisterSelector(this.registeredSelector, this, this.getDevice());
            }

            this.lonNetwork().nvManager().registerSelector(newSel, this, this.getDevice());
            this.registeredSelector = newSel;
         }
      }
   }

   private void unregisterSelector() {
      if (this.registeredSelector != UNREGISTORED_SELECTOR) {
         this.lonNetwork().nvManager().unregisterSelector(this.registeredSelector, this, this.getDevice());
         this.registeredSelector = UNREGISTORED_SELECTOR;
      }
   }

   public Tuning getTuning() {
      return this.tuning;
   }

   public boolean isFatalFault() {
      return this.getDevice().isFatalFault();
   }

   public BStatus getStatus() {
      return this.getDevice().getStatus();
   }

   public boolean isSubscribedDesired() {
      return this.isSubscribed();
   }

   public BReadWriteMode getMode() {
      return this.isWriteable() ? BReadWriteMode.writeonly : BReadWriteMode.readonly;
   }

   public void readSubscribed(Context cx) {
      super.readSubscribed();
   }

   public void readUnsubscribed(Context cx) {
      super.readSubscribed();
   }

   public boolean write(Context cx) {
      this.forceWrite();
      return false;
   }

   public void setStale(boolean s, Context cx) {
      if (this.getData().hasProxies()) {
         this.getData().markStale(s, cx);
      }
   }
}
