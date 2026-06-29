package com.tridium.lonworks.datatypes;

import com.tridium.lonworks.enums.BLonRouterMode;
import com.tridium.lonworks.enums.BLonRouterType;
import javax.baja.lonworks.datatypes.BNeuronId;
import javax.baja.lonworks.datatypes.BSubnetNode;
import javax.baja.lonworks.enums.BLonNodeState;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BStruct;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "devName",
      type = "String",
      defaultValue = ""
   ), @NiagaraProperty(
      name = "routerType",
      type = "BLonRouterType",
      defaultValue = "BLonRouterType.unknown"
   ), @NiagaraProperty(
      name = "mode",
      type = "BLonRouterMode",
      defaultValue = "BLonRouterMode.unknown"
   ), @NiagaraProperty(
      name = "state",
      type = "BLonNodeState",
      defaultValue = "BLonNodeState.unknown"
   ), @NiagaraProperty(
      name = "nearChannel",
      type = "int",
      defaultValue = "0"
   ), @NiagaraProperty(
      name = "nearAddress",
      type = "BSubnetNode",
      defaultValue = "BSubnetNode.make(0,0)"
   ), @NiagaraProperty(
      name = "farChannel",
      type = "int",
      defaultValue = "0"
   ), @NiagaraProperty(
      name = "farAddress",
      type = "BSubnetNode",
      defaultValue = "BSubnetNode.make(0,0)"
   ), @NiagaraProperty(
      name = "neuronId",
      type = "BNeuronId",
      defaultValue = "BNeuronId.DEFAULT"
   ), @NiagaraProperty(
      name = "farNeuronId",
      type = "BNeuronId",
      defaultValue = "BNeuronId.DEFAULT"
   )})
public class BRouterEntry extends BStruct {
   public static final Property devName = newProperty(0, "", null);
   public static final Property routerType = newProperty(0, BLonRouterType.unknown, null);
   public static final Property mode = newProperty(0, BLonRouterMode.unknown, null);
   public static final Property state = newProperty(0, BLonNodeState.unknown, null);
   public static final Property nearChannel = newProperty(0, 0, null);
   public static final Property nearAddress = newProperty(0, BSubnetNode.make(0, 0), null);
   public static final Property farChannel = newProperty(0, 0, null);
   public static final Property farAddress = newProperty(0, BSubnetNode.make(0, 0), null);
   public static final Property neuronId = newProperty(0, BNeuronId.DEFAULT, null);
   public static final Property farNeuronId = newProperty(0, BNeuronId.DEFAULT, null);
   public static final Type TYPE = Sys.loadType(BRouterEntry.class);

   public String getDevName() {
      return this.getString(devName);
   }

   public void setDevName(String v) {
      this.setString(devName, v, null);
   }

   public BLonRouterType getRouterType() {
      return (BLonRouterType)this.get(routerType);
   }

   public void setRouterType(BLonRouterType v) {
      this.set(routerType, v, null);
   }

   public BLonRouterMode getMode() {
      return (BLonRouterMode)this.get(mode);
   }

   public void setMode(BLonRouterMode v) {
      this.set(mode, v, null);
   }

   public BLonNodeState getState() {
      return (BLonNodeState)this.get(state);
   }

   public void setState(BLonNodeState v) {
      this.set(state, v, null);
   }

   public int getNearChannel() {
      return this.getInt(nearChannel);
   }

   public void setNearChannel(int v) {
      this.setInt(nearChannel, v, null);
   }

   public BSubnetNode getNearAddress() {
      return (BSubnetNode)this.get(nearAddress);
   }

   public void setNearAddress(BSubnetNode v) {
      this.set(nearAddress, v, null);
   }

   public int getFarChannel() {
      return this.getInt(farChannel);
   }

   public void setFarChannel(int v) {
      this.setInt(farChannel, v, null);
   }

   public BSubnetNode getFarAddress() {
      return (BSubnetNode)this.get(farAddress);
   }

   public void setFarAddress(BSubnetNode v) {
      this.set(farAddress, v, null);
   }

   public BNeuronId getNeuronId() {
      return (BNeuronId)this.get(neuronId);
   }

   public void setNeuronId(BNeuronId v) {
      this.set(neuronId, v, null);
   }

   public BNeuronId getFarNeuronId() {
      return (BNeuronId)this.get(farNeuronId);
   }

   public void setFarNeuronId(BNeuronId v) {
      this.set(farNeuronId, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BRouterEntry(
      String devName,
      BLonRouterType rtrType,
      BLonRouterMode mode,
      BLonNodeState state,
      int nearChannel,
      BSubnetNode nearAddress,
      int farChannel,
      BSubnetNode farAddress,
      BNeuronId neuronId,
      BNeuronId farNeuronId
   ) {
      this.setDevName(devName);
      this.setRouterType(rtrType);
      this.setMode(mode);
      this.setState(state);
      this.setNearChannel(nearChannel);
      this.setNearAddress(nearAddress);
      this.setFarChannel(farChannel);
      this.setFarAddress(farAddress);
      this.setNeuronId(neuronId);
      this.setFarNeuronId(farNeuronId);
   }

   public String toString(Context context) {
      return this.getDevName()
         + ","
         + this.getState()
         + ","
         + this.getNearChannel()
         + ":"
         + this.getNearAddress()
         + ","
         + this.getFarChannel()
         + ":"
         + this.getFarAddress()
         + ","
         + this.getNeuronId()
         + ","
         + this.getFarNeuronId();
   }

   public String getUtilName() {
      return this.getNearChannel() + ":" + this.getNearAddress() + "," + this.getFarChannel() + ":" + this.getFarAddress();
   }

   public BRouterEntry() {
   }
}
