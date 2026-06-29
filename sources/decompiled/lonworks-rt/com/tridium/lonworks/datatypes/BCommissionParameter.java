package com.tridium.lonworks.datatypes;

import com.tridium.lonworks.util.LonStringUtil;
import javax.baja.lonworks.datatypes.BNeuronId;
import javax.baja.lonworks.datatypes.BSubnetNode;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BSimple;
import javax.baja.sys.BStruct;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "servicePin",
      type = "boolean",
      defaultValue = "false"
   ), @NiagaraProperty(
      name = "subnetNode",
      type = "BSubnetNode",
      defaultValue = "BSubnetNode.DEFAULT",
      flags = 4
   ), @NiagaraProperty(
      name = "neuronId",
      type = "BNeuronId",
      defaultValue = "BNeuronId.DEFAULT",
      flags = 4
   ), @NiagaraProperty(
      name = "subnetNodeArray",
      type = "String",
      defaultValue = "",
      flags = 4
   )})
public class BCommissionParameter extends BStruct {
   public static final Property servicePin = newProperty(0, false, null);
   public static final Property subnetNode = newProperty(4, BSubnetNode.DEFAULT, null);
   public static final Property neuronId = newProperty(4, BNeuronId.DEFAULT, null);
   public static final Property subnetNodeArray = newProperty(4, "", null);
   public static final Type TYPE = Sys.loadType(BCommissionParameter.class);

   public boolean getServicePin() {
      return this.getBoolean(servicePin);
   }

   public void setServicePin(boolean v) {
      this.setBoolean(servicePin, v, null);
   }

   public BSubnetNode getSubnetNode() {
      return (BSubnetNode)this.get(subnetNode);
   }

   public void setSubnetNode(BSubnetNode v) {
      this.set(subnetNode, v, null);
   }

   public BNeuronId getNeuronId() {
      return (BNeuronId)this.get(neuronId);
   }

   public void setNeuronId(BNeuronId v) {
      this.set(neuronId, v, null);
   }

   public String getSubnetNodeArray() {
      return this.getString(subnetNodeArray);
   }

   public void setSubnetNodeArray(String v) {
      this.setString(subnetNodeArray, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BCommissionParameter() {
   }

   public BCommissionParameter(BSubnetNode sn, BNeuronId nid, boolean servicePin) {
      this.setServicePin(servicePin);
      this.setSubnetNode(sn);
      this.setNeuronId(nid);
   }

   public BCommissionParameter(BSubnetNode sn, boolean servicePin) {
      this.setServicePin(servicePin);
      this.setSubnetNode(sn);
   }

   public BCommissionParameter(BSubnetNode[] sn) {
      this.setSubnetNodeArray(LonStringUtil.toString((BSimple[])sn));
   }

   public BSubnetNode[] getSubnetNodes() {
      return this.getSubnetNodeArray().length() == 0
         ? new BSubnetNode[]{this.getSubnetNode()}
         : (BSubnetNode[])LonStringUtil.getSimpleArray(this.getSubnetNodeArray(), BSubnetNode.DEFAULT);
   }
}
