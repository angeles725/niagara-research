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
      name = "value",
      type = "String",
      defaultValue = ""
   ), @NiagaraProperty(
      name = "description",
      type = "String",
      defaultValue = "BDiscoveryConfig.DESCRIPTION_DEFAULT"
   ), @NiagaraProperty(
      name = "typeSpecs",
      type = "String",
      defaultValue = ""
   )})
public class BDiscoveryConfig extends BStruct {
   public static final Property objectName = newProperty(0, "", null);
   public static final Property objectId = newProperty(0, BBacnetObjectIdentifier.DEFAULT, null);
   public static final Property value = newProperty(0, "", null);
   public static final Property description = newProperty(0, "n/a", null);
   public static final Property typeSpecs = newProperty(0, "", null);
   public static final Type TYPE = Sys.loadType(BDiscoveryConfig.class);
   private static final String DESCRIPTION_DEFAULT = "n/a";

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

   public String getValue() {
      return this.getString(value);
   }

   public void setValue(String v) {
      this.setString(value, v, null);
   }

   public String getDescription() {
      return this.getString(description);
   }

   public void setDescription(String v) {
      this.setString(description, v, null);
   }

   public String getTypeSpecs() {
      return this.getString(typeSpecs);
   }

   public void setTypeSpecs(String v) {
      this.setString(typeSpecs, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BDiscoveryConfig() {
   }

   public BDiscoveryConfig(String objectName, BBacnetObjectIdentifier objectId) {
      this.setObjectName(objectName);
      this.setObjectId(objectId);
   }

   public String toString(Context cx) {
      StringBuilder sb = new StringBuilder();
      sb.append("DiscCfg:").append(this.getObjectName()).append("[" + this.getObjectId() + "]").append('=').append(this.getValue());
      if (!description.isEquivalentToDefaultValue(this.get(description))) {
         sb.append(" :").append(this.getDescription());
      }

      return sb.toString();
   }
}
