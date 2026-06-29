package com.tridium.lonworks.datatypes;

import javax.baja.lonworks.datatypes.BSubnetNode;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BStruct;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "dbDevSubnetNode",
      type = "BSubnetNode",
      defaultValue = "BSubnetNode.DEFAULT"
   ), @NiagaraProperty(
      name = "deviceEntry",
      type = "BDeviceEntry",
      defaultValue = "new BDeviceEntry()"
   )})
public class BMatchParameter extends BStruct {
   public static final Property dbDevSubnetNode = newProperty(0, BSubnetNode.DEFAULT, null);
   public static final Property deviceEntry = newProperty(0, new BDeviceEntry(), null);
   public static final Type TYPE = Sys.loadType(BMatchParameter.class);

   public BSubnetNode getDbDevSubnetNode() {
      return (BSubnetNode)this.get(dbDevSubnetNode);
   }

   public void setDbDevSubnetNode(BSubnetNode v) {
      this.set(dbDevSubnetNode, v, null);
   }

   public BDeviceEntry getDeviceEntry() {
      return (BDeviceEntry)this.get(deviceEntry);
   }

   public void setDeviceEntry(BDeviceEntry v) {
      this.set(deviceEntry, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BMatchParameter() {
   }

   public BMatchParameter(BSubnetNode sn, BDeviceEntry deviceEntry) {
      this.setDbDevSubnetNode(sn);
      this.setDeviceEntry(deviceEntry);
   }
}
