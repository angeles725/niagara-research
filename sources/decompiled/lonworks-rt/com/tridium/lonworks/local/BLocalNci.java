package com.tridium.lonworks.local;

import java.util.logging.Level;
import javax.baja.lonworks.BINetworkVariable;
import javax.baja.lonworks.BLocalLonDevice;
import javax.baja.lonworks.BLonComponent;
import javax.baja.lonworks.datatypes.BNcProps;
import javax.baja.lonworks.datatypes.BNvConfigData;
import javax.baja.lonworks.enums.BLonSnvtType;
import javax.baja.lonworks.util.SnvtUtil;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraActions;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Action;
import javax.baja.sys.BComponent;
import javax.baja.sys.BIcon;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "ncProps",
      type = "BNcProps",
      defaultValue = "new BNcProps()"
   ), @NiagaraProperty(
      name = "nvConfigData",
      type = "BNvConfigData",
      defaultValue = "new BNvConfigData()"
   ), @NiagaraProperty(
      name = "selfDoc",
      type = "String",
      defaultValue = ""
   )})
@NiagaraActions({@NiagaraAction(
      name = "forceRead",
      flags = 4,
      override = true
   ), @NiagaraAction(
      name = "forceWrite",
      flags = 4,
      override = true
   )})
public class BLocalNci extends BLonComponent implements BINetworkVariable {
   public static final Property ncProps = newProperty(0, new BNcProps(), null);
   public static final Property nvConfigData = newProperty(0, new BNvConfigData(), null);
   public static final Property selfDoc = newProperty(0, "", null);
   public static final Action forceRead = newAction(4, null);
   public static final Action forceWrite = newAction(4, null);
   public static final Type TYPE = Sys.loadType(BLocalNci.class);
   private static final BIcon icon = BIcon.make("module://lonworks/com/tridium/lonworks/ui/icons/nci.png");

   public BNcProps getNcProps() {
      return (BNcProps)this.get(ncProps);
   }

   public void setNcProps(BNcProps v) {
      this.set(ncProps, v, null);
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

   @Override
   public Type getType() {
      return TYPE;
   }

   public BIcon getIcon() {
      return icon;
   }

   @Override
   public boolean isParentLegal(BComponent parent) {
      return parent instanceof BLocalLonDevice;
   }

   @Override
   public void changed(Property prop, Context context) {
      super.changed(prop, context);
      if (this.isRunning() && context != BLocalLonDevice.noInfoChange) {
         if (prop == ncProps) {
            this.nvChanged();
         }
      }
   }

   private void nvChanged() {
      ((BLocalLonDevice)this.getParent()).nvChanged(this);
   }

   @Override
   public void doForceWrite() {
   }

   @Override
   public void doForceRead() {
   }

   @Override
   public boolean isWriteable() {
      return false;
   }

   @Override
   public int getNvIndex() {
      return this.getNcProps().getNvIndex();
   }

   @Override
   public void setNvIndex(int nvIndex) {
      this.getNcProps().setNvIndex(nvIndex);
   }

   @Override
   public int getSnvtType() {
      return this.getNcProps().getSnvtType();
   }

   @Override
   public void setUnbound() {
      this.getNvConfigData().setUnbound(this.getNcProps().getNvIndex());
      this.getNcProps().setUnbound();
   }

   @Override
   public boolean isLocalNci() {
      return true;
   }

   @Override
   public void receiveUpdate(byte[] nvData) {
      try {
         this.getData().fromNetBytes(nvData);
         this.getData().readOk();
      } catch (Throwable var3) {
         this.getData().readFail(var3.toString());
         this.lonNetwork()
            .log()
            .log(Level.SEVERE, "Could not decode nv update data " + this.getParent().getDisplayName(null) + ":" + this.getDisplayName(null), var3);
      }
   }

   public void configure(BLonSnvtType type, String sDoc) {
      BNcProps ncProps = this.getNcProps();
      int snvtType = type.getOrdinal();
      this.setData(SnvtUtil.getLonData(snvtType, 2));
      ncProps.setInt(BNcProps.snvtType, snvtType, BLocalLonDevice.noInfoChange);
   }
}
