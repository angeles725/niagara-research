package com.tridium.lonworks.datatypes;

import com.tridium.lonworks.util.LonStringUtil;
import javax.baja.lonworks.datatypes.BSubnetNode;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BBlob;
import javax.baja.sys.BSimple;
import javax.baja.sys.BStruct;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "appFile",
      type = "BBlob",
      defaultValue = "BBlob.DEFAULT"
   ), @NiagaraProperty(
      name = "nvDirFile",
      type = "BBlob",
      defaultValue = "BBlob.DEFAULT"
   ), @NiagaraProperty(
      name = "subnetNodeArray",
      type = "String",
      defaultValue = "",
      flags = 4
   ), @NiagaraProperty(
      name = "bind",
      type = "boolean",
      defaultValue = "true"
   )})
public class BAppDownloadParameter extends BStruct {
   public static final Property appFile = newProperty(0, BBlob.DEFAULT, null);
   public static final Property nvDirFile = newProperty(0, BBlob.DEFAULT, null);
   public static final Property subnetNodeArray = newProperty(4, "", null);
   public static final Property bind = newProperty(0, true, null);
   public static final Type TYPE = Sys.loadType(BAppDownloadParameter.class);

   public BBlob getAppFile() {
      return (BBlob)this.get(appFile);
   }

   public void setAppFile(BBlob v) {
      this.set(appFile, v, null);
   }

   public BBlob getNvDirFile() {
      return (BBlob)this.get(nvDirFile);
   }

   public void setNvDirFile(BBlob v) {
      this.set(nvDirFile, v, null);
   }

   public String getSubnetNodeArray() {
      return this.getString(subnetNodeArray);
   }

   public void setSubnetNodeArray(String v) {
      this.setString(subnetNodeArray, v, null);
   }

   public boolean getBind() {
      return this.getBoolean(bind);
   }

   public void setBind(boolean v) {
      this.setBoolean(bind, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BAppDownloadParameter() {
   }

   public BAppDownloadParameter(BSubnetNode[] sn) {
      this.setSubnetNodeArray(LonStringUtil.toString((BSimple[])sn));
   }

   public BSubnetNode[] getSubnetNodes() {
      return (BSubnetNode[])LonStringUtil.getSimpleArray(this.getSubnetNodeArray(), BSubnetNode.DEFAULT);
   }
}
