package com.tridium.opc.client;

import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BComponent;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "address",
      type = "String",
      defaultValue = "localhost",
      flags = 2
   ), @NiagaraProperty(
      name = "description",
      type = "String",
      defaultValue = "",
      flags = 2
   ), @NiagaraProperty(
      name = "classId",
      type = "String",
      defaultValue = "",
      flags = 2
   ), @NiagaraProperty(
      name = "progId",
      type = "String",
      defaultValue = "",
      flags = 2
   ), @NiagaraProperty(
      name = "versionIndependentProgId",
      type = "String",
      defaultValue = "",
      flags = 2
   ), @NiagaraProperty(
      name = "catId",
      type = "String",
      defaultValue = "",
      flags = 2
   )})
public class BOpcDeviceDiscoveryResult extends BComponent {
   public static final Property address = newProperty(2, "localhost", null);
   public static final Property description = newProperty(2, "", null);
   public static final Property classId = newProperty(2, "", null);
   public static final Property progId = newProperty(2, "", null);
   public static final Property versionIndependentProgId = newProperty(2, "", null);
   public static final Property catId = newProperty(2, "", null);
   public static final Type TYPE = Sys.loadType(BOpcDeviceDiscoveryResult.class);

   public String getAddress() {
      return this.getString(address);
   }

   public void setAddress(String v) {
      this.setString(address, v, null);
   }

   public String getDescription() {
      return this.getString(description);
   }

   public void setDescription(String v) {
      this.setString(description, v, null);
   }

   public String getClassId() {
      return this.getString(classId);
   }

   public void setClassId(String v) {
      this.setString(classId, v, null);
   }

   public String getProgId() {
      return this.getString(progId);
   }

   public void setProgId(String v) {
      this.setString(progId, v, null);
   }

   public String getVersionIndependentProgId() {
      return this.getString(versionIndependentProgId);
   }

   public void setVersionIndependentProgId(String v) {
      this.setString(versionIndependentProgId, v, null);
   }

   public String getCatId() {
      return this.getString(catId);
   }

   public void setCatId(String v) {
      this.setString(catId, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BOpcDeviceDiscoveryResult() {
   }

   public BOpcDeviceDiscoveryResult(String desc) {
      this.setDescription(desc);
   }

   public BOpcDeviceDiscoveryResult(String name, String classid, String progId, String verIndProgid, String host, String catid) {
      this.setAddress(host);
      this.setDescription(name);
      this.setClassId(classid);
      this.setProgId(progId);
      this.setVersionIndependentProgId(verIndProgid);
      this.setCatId(catid);
   }
}
