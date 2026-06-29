package com.tridium.lonworks.datatypes;

import com.tridium.lonworks.enums.BUtilCommandEnum;
import javax.baja.lonworks.datatypes.BNeuronId;
import javax.baja.lonworks.datatypes.BSubnetNode;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BComponent;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "command",
      type = "BUtilCommandEnum",
      defaultValue = "BUtilCommandEnum.status",
      flags = 2
   ), @NiagaraProperty(
      name = "isDevice",
      type = "boolean",
      defaultValue = "true"
   ), @NiagaraProperty(
      name = "subnetNodeId",
      type = "BSubnetNode",
      defaultValue = "BSubnetNode.DEFAULT",
      flags = 64
   ), @NiagaraProperty(
      name = "neuronId",
      type = "BNeuronId",
      defaultValue = "BNeuronId.DEFAULT",
      flags = 64
   ), @NiagaraProperty(
      name = "commandData",
      type = "String",
      defaultValue = ""
   ), @NiagaraProperty(
      name = "displayName",
      type = "String",
      defaultValue = ""
   ), @NiagaraProperty(
      name = "memAddr",
      type = "int",
      defaultValue = "-1"
   ), @NiagaraProperty(
      name = "count",
      type = "int",
      defaultValue = "-1"
   ), @NiagaraProperty(
      name = "fileNum",
      type = "int",
      defaultValue = "-1"
   )})
public class BUtilitiesCommand extends BComponent {
   public static final Property command = newProperty(2, BUtilCommandEnum.status, null);
   public static final Property isDevice = newProperty(0, true, null);
   public static final Property subnetNodeId = newProperty(64, BSubnetNode.DEFAULT, null);
   public static final Property neuronId = newProperty(64, BNeuronId.DEFAULT, null);
   public static final Property commandData = newProperty(0, "", null);
   public static final Property displayName = newProperty(0, "", null);
   public static final Property memAddr = newProperty(0, -1, null);
   public static final Property count = newProperty(0, -1, null);
   public static final Property fileNum = newProperty(0, -1, null);
   public static final Type TYPE = Sys.loadType(BUtilitiesCommand.class);

   public BUtilCommandEnum getCommand() {
      return (BUtilCommandEnum)this.get(command);
   }

   public void setCommand(BUtilCommandEnum v) {
      this.set(command, v, null);
   }

   public boolean getIsDevice() {
      return this.getBoolean(isDevice);
   }

   public void setIsDevice(boolean v) {
      this.setBoolean(isDevice, v, null);
   }

   public BSubnetNode getSubnetNodeId() {
      return (BSubnetNode)this.get(subnetNodeId);
   }

   public void setSubnetNodeId(BSubnetNode v) {
      this.set(subnetNodeId, v, null);
   }

   public BNeuronId getNeuronId() {
      return (BNeuronId)this.get(neuronId);
   }

   public void setNeuronId(BNeuronId v) {
      this.set(neuronId, v, null);
   }

   public String getCommandData() {
      return this.getString(commandData);
   }

   public void setCommandData(String v) {
      this.setString(commandData, v, null);
   }

   public String getDisplayName() {
      return this.getString(displayName);
   }

   public void setDisplayName(String v) {
      this.setString(displayName, v, null);
   }

   public int getMemAddr() {
      return this.getInt(memAddr);
   }

   public void setMemAddr(int v) {
      this.setInt(memAddr, v, null);
   }

   public int getCount() {
      return this.getInt(count);
   }

   public void setCount(int v) {
      this.setInt(count, v, null);
   }

   public int getFileNum() {
      return this.getInt(fileNum);
   }

   public void setFileNum(int v) {
      this.setInt(fileNum, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BUtilitiesCommand() {
   }

   public BUtilitiesCommand(BUtilCommandEnum command) {
      this.setCommand(command);
      this.setIsDevice(false);
   }

   public BUtilitiesCommand(BUtilCommandEnum command, boolean isDevice, BSubnetNode sn, BNeuronId nid, String name) {
      this.setCommand(command);
      this.setSubnetNodeId(sn);
      this.setNeuronId(nid);
      this.setDisplayName(name);
      this.setIsDevice(isDevice);
   }

   public boolean isDevice() {
      return this.getIsDevice();
   }
}
