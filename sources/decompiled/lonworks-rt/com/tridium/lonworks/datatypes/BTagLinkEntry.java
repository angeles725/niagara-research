package com.tridium.lonworks.datatypes;

import com.tridium.lonworks.enums.BLonLinkStatus;
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
      name = "linkStatus",
      type = "BLonLinkStatus",
      defaultValue = "BLonLinkStatus.unbound"
   ), @NiagaraProperty(
      name = "outputDevice",
      type = "String",
      defaultValue = ""
   ), @NiagaraProperty(
      name = "outputTag",
      type = "String",
      defaultValue = ""
   ), @NiagaraProperty(
      name = "inputDevice",
      type = "String",
      defaultValue = ""
   ), @NiagaraProperty(
      name = "inputTag",
      type = "String",
      defaultValue = ""
   )})
public class BTagLinkEntry extends BStruct {
   public static final Property linkStatus = newProperty(0, BLonLinkStatus.unbound, null);
   public static final Property outputDevice = newProperty(0, "", null);
   public static final Property outputTag = newProperty(0, "", null);
   public static final Property inputDevice = newProperty(0, "", null);
   public static final Property inputTag = newProperty(0, "", null);
   public static final Type TYPE = Sys.loadType(BTagLinkEntry.class);

   public BLonLinkStatus getLinkStatus() {
      return (BLonLinkStatus)this.get(linkStatus);
   }

   public void setLinkStatus(BLonLinkStatus v) {
      this.set(linkStatus, v, null);
   }

   public String getOutputDevice() {
      return this.getString(outputDevice);
   }

   public void setOutputDevice(String v) {
      this.setString(outputDevice, v, null);
   }

   public String getOutputTag() {
      return this.getString(outputTag);
   }

   public void setOutputTag(String v) {
      this.setString(outputTag, v, null);
   }

   public String getInputDevice() {
      return this.getString(inputDevice);
   }

   public void setInputDevice(String v) {
      this.setString(inputDevice, v, null);
   }

   public String getInputTag() {
      return this.getString(inputTag);
   }

   public void setInputTag(String v) {
      this.setString(inputTag, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BTagLinkEntry() {
   }

   public BTagLinkEntry(BLonLinkStatus linkStatus, String outputDevice, String outputTag, String inputDevice, String inputTag) {
      this.setLinkStatus(linkStatus);
      this.setOutputDevice(outputDevice);
      this.setOutputTag(outputTag);
      this.setInputDevice(inputDevice);
      this.setInputTag(inputTag);
   }

   public String toString(Context c) {
      return this.getLinkStatus() + "," + this.getOutputDevice() + "," + this.getOutputTag() + "," + this.getInputDevice() + "," + this.getInputTag();
   }

   public boolean sameOutput(BTagLinkEntry other) {
      return this.getOutputDevice().equals(other.getOutputDevice()) && this.getOutputTag().equals(other.getOutputTag());
   }
}
