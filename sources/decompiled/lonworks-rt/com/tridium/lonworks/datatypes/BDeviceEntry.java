package com.tridium.lonworks.datatypes;

import javax.baja.lonworks.datatypes.BNeuronId;
import javax.baja.lonworks.datatypes.BProgramId;
import javax.baja.lonworks.datatypes.BSubnetNode;
import javax.baja.lonworks.enums.BLonMfgId;
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
      name = "channelId",
      type = "int",
      defaultValue = "0"
   ), @NiagaraProperty(
      name = "devName",
      type = "String",
      defaultValue = ""
   ), @NiagaraProperty(
      name = "state",
      type = "BLonNodeState",
      defaultValue = "BLonNodeState.unknown"
   ), @NiagaraProperty(
      name = "subnet",
      type = "int",
      defaultValue = "0"
   ), @NiagaraProperty(
      name = "node",
      type = "int",
      defaultValue = "0"
   ), @NiagaraProperty(
      name = "mfgId",
      type = "BLonMfgId",
      defaultValue = "BLonMfgId.unknown"
   ), @NiagaraProperty(
      name = "programId",
      type = "BProgramId",
      defaultValue = "BProgramId.DEFAULT"
   ), @NiagaraProperty(
      name = "neuronId",
      type = "BNeuronId",
      defaultValue = "BNeuronId.DEFAULT"
   ), @NiagaraProperty(
      name = "authenticate",
      type = "boolean",
      defaultValue = "false"
   ), @NiagaraProperty(
      name = "workingDomain",
      type = "int",
      defaultValue = "0"
   )})
public class BDeviceEntry extends BStruct {
   public static final Property channelId = newProperty(0, 0, null);
   public static final Property devName = newProperty(0, "", null);
   public static final Property state = newProperty(0, BLonNodeState.unknown, null);
   public static final Property subnet = newProperty(0, 0, null);
   public static final Property node = newProperty(0, 0, null);
   public static final Property mfgId = newProperty(0, BLonMfgId.unknown, null);
   public static final Property programId = newProperty(0, BProgramId.DEFAULT, null);
   public static final Property neuronId = newProperty(0, BNeuronId.DEFAULT, null);
   public static final Property authenticate = newProperty(0, false, null);
   public static final Property workingDomain = newProperty(0, 0, null);
   public static final Type TYPE = Sys.loadType(BDeviceEntry.class);

   public int getChannelId() {
      return this.getInt(channelId);
   }

   public void setChannelId(int v) {
      this.setInt(channelId, v, null);
   }

   public String getDevName() {
      return this.getString(devName);
   }

   public void setDevName(String v) {
      this.setString(devName, v, null);
   }

   public BLonNodeState getState() {
      return (BLonNodeState)this.get(state);
   }

   public void setState(BLonNodeState v) {
      this.set(state, v, null);
   }

   public int getSubnet() {
      return this.getInt(subnet);
   }

   public void setSubnet(int v) {
      this.setInt(subnet, v, null);
   }

   public int getNode() {
      return this.getInt(node);
   }

   public void setNode(int v) {
      this.setInt(node, v, null);
   }

   public BLonMfgId getMfgId() {
      return (BLonMfgId)this.get(mfgId);
   }

   public void setMfgId(BLonMfgId v) {
      this.set(mfgId, v, null);
   }

   public BProgramId getProgramId() {
      return (BProgramId)this.get(programId);
   }

   public void setProgramId(BProgramId v) {
      this.set(programId, v, null);
   }

   public BNeuronId getNeuronId() {
      return (BNeuronId)this.get(neuronId);
   }

   public void setNeuronId(BNeuronId v) {
      this.set(neuronId, v, null);
   }

   public boolean getAuthenticate() {
      return this.getBoolean(authenticate);
   }

   public void setAuthenticate(boolean v) {
      this.setBoolean(authenticate, v, null);
   }

   public int getWorkingDomain() {
      return this.getInt(workingDomain);
   }

   public void setWorkingDomain(int v) {
      this.setInt(workingDomain, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BDeviceEntry(
      String devName, BLonNodeState state, int subnet, int node, BNeuronId neuronId, BProgramId programId, int channelId, boolean auth, int wrkDmn
   ) {
      this.setDevName(devName);
      this.setState(state);
      this.setSubnet(subnet);
      this.setNode(node);
      this.setMfgId(programId.getMfgId());
      this.setNeuronId(neuronId);
      this.setProgramId(programId);
      this.setChannelId(channelId);
      this.setAuthenticate(auth);
      this.setWorkingDomain(wrkDmn);
   }

   public BDeviceEntry() {
   }

   public BSubnetNode getSubnetNodeId() {
      return BSubnetNode.make(this.getSubnet(), this.getNode());
   }

   public String toString(Context context) {
      return this.getDevName()
         + ","
         + this.getState()
         + ","
         + this.getChannelId()
         + ","
         + this.getSubnet()
         + "/"
         + this.getNode()
         + ","
         + this.getMfgId().getConvertedName()
         + ","
         + this.getNeuronId()
         + ","
         + this.getProgramId()
         + ",";
   }

   public String getUtilName() {
      return this.getSubnet() + "/" + this.getNode() + "," + this.getMfgId().getConvertedName() + "," + this.getProgramId();
   }
}
