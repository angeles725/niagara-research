package com.tridium.lonworks.datatypes;

import com.tridium.lonworks.enums.BLonLinkStatus;
import javax.baja.lonworks.enums.BLonLinkType;
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
      name = "selector",
      type = "int",
      defaultValue = "0"
   ), @NiagaraProperty(
      name = "linkStatus",
      type = "BLonLinkStatus",
      defaultValue = "BLonLinkStatus.unbound"
   ), @NiagaraProperty(
      name = "hubDevice",
      type = "String",
      defaultValue = ""
   ), @NiagaraProperty(
      name = "hubNv",
      type = "String",
      defaultValue = ""
   ), @NiagaraProperty(
      name = "targetDevice",
      type = "String",
      defaultValue = ""
   ), @NiagaraProperty(
      name = "targetNv",
      type = "String",
      defaultValue = ""
   ), @NiagaraProperty(
      name = "linkType",
      type = "String",
      defaultValue = "BLonLinkType.standard.getTag()"
   )})
public class BLinkEntry extends BStruct {
   public static final Property selector = newProperty(0, 0, null);
   public static final Property linkStatus = newProperty(0, BLonLinkStatus.unbound, null);
   public static final Property hubDevice = newProperty(0, "", null);
   public static final Property hubNv = newProperty(0, "", null);
   public static final Property targetDevice = newProperty(0, "", null);
   public static final Property targetNv = newProperty(0, "", null);
   public static final Property linkType = newProperty(0, BLonLinkType.standard.getTag(), null);
   public static final Type TYPE = Sys.loadType(BLinkEntry.class);

   public BLinkEntry() {
   }

   public BLinkEntry(int selector, BLonLinkStatus linkStatus, String hubDevice, String hubNv, String targetDevice, String targetNv, String linkType) {
      this.setSelector(selector);
      this.setLinkStatus(linkStatus);
      this.setHubDevice(hubDevice);
      this.setHubNv(hubNv);
      this.setTargetDevice(targetDevice);
      this.setTargetNv(targetNv);
      this.setLinkType(linkType);
   }

   public int getSelector() {
      return this.getInt(selector);
   }

   public void setSelector(int v) {
      this.setInt(selector, v, null);
   }

   public BLonLinkStatus getLinkStatus() {
      return (BLonLinkStatus)this.get(linkStatus);
   }

   public void setLinkStatus(BLonLinkStatus v) {
      this.set(linkStatus, v, null);
   }

   public String getHubDevice() {
      return this.getString(hubDevice);
   }

   public void setHubDevice(String v) {
      this.setString(hubDevice, v, null);
   }

   public String getHubNv() {
      return this.getString(hubNv);
   }

   public void setHubNv(String v) {
      this.setString(hubNv, v, null);
   }

   public String getTargetDevice() {
      return this.getString(targetDevice);
   }

   public void setTargetDevice(String v) {
      this.setString(targetDevice, v, null);
   }

   public String getTargetNv() {
      return this.getString(targetNv);
   }

   public void setTargetNv(String v) {
      this.setString(targetNv, v, null);
   }

   public String getLinkType() {
      return this.getString(linkType);
   }

   public void setLinkType(String v) {
      this.setString(linkType, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public String toString(Context c) {
      return "0x"
         + Integer.toString(this.getSelector(), 16)
         + ","
         + this.getLinkStatus()
         + ","
         + this.getHubDevice()
         + ","
         + this.getHubNv()
         + ","
         + this.getTargetDevice()
         + ","
         + this.getTargetNv()
         + ","
         + this.getLinkType();
   }

   public boolean isProxy() {
      return this.getHubDevice().equals("LocalDev") || this.getTargetDevice().equals("LocalDev");
   }
}
