package com.tridium.bacnet.job;

import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
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
      name = "objectName",
      type = "String",
      defaultValue = ""
   ), @NiagaraProperty(
      name = "objectId",
      type = "BBacnetObjectIdentifier",
      defaultValue = "BBacnetObjectIdentifier.DEFAULT"
   ), @NiagaraProperty(
      name = "dataType",
      type = "String",
      defaultValue = ""
   ), @NiagaraProperty(
      name = "description",
      type = "String",
      defaultValue = ""
   )})
public class BDiscoveryLog extends BStruct {
   public static final Property objectName = newProperty(0, "", null);
   public static final Property objectId = newProperty(0, BBacnetObjectIdentifier.DEFAULT, null);
   public static final Property dataType = newProperty(0, "", null);
   public static final Property description = newProperty(0, "", null);
   public static final Type TYPE = Sys.loadType(BDiscoveryLog.class);

   public String getObjectName() {
      return this.getString(objectName);
   }

   public void setObjectName(String v) {
      this.setString(objectName, v, null);
   }

   public BBacnetObjectIdentifier getObjectId() {
      return (BBacnetObjectIdentifier)this.get(objectId);
   }

   public void setObjectId(BBacnetObjectIdentifier v) {
      this.set(objectId, v, null);
   }

   public String getDataType() {
      return this.getString(dataType);
   }

   public void setDataType(String v) {
      this.setString(dataType, v, null);
   }

   public String getDescription() {
      return this.getString(description);
   }

   public void setDescription(String v) {
      this.setString(description, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BDiscoveryLog() {
   }

   public BDiscoveryLog(String objectName, BBacnetObjectIdentifier objectId, String dataType) {
      this.setObjectName(objectName);
      this.setObjectId(objectId);
      this.setDataType(dataType);
   }

   public String toString(Context cx) {
      StringBuilder sb = new StringBuilder();
      sb.append("DiscLog:").append(this.getObjectName()).append("[" + this.getObjectId() + "]").append(':').append(this.getDataType());
      return sb.toString();
   }
}
