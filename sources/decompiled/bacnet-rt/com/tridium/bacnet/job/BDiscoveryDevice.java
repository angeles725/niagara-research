package com.tridium.bacnet.job;

import com.tridium.bacnet.services.unconfirmed.IAmRequest;
import javax.baja.bacnet.datatypes.BBacnetAddress;
import javax.baja.bacnet.datatypes.BBacnetBitString;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.enums.BBacnetSegmentation;
import javax.baja.bacnet.enums.BCharacterSetEncoding;
import javax.baja.bacnet.util.BacnetBitStringUtil;
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
      name = "deviceName",
      type = "String",
      defaultValue = ""
   ), @NiagaraProperty(
      name = "objectId",
      type = "BBacnetObjectIdentifier",
      defaultValue = "BBacnetObjectIdentifier.DEFAULT"
   ), @NiagaraProperty(
      name = "maxApduLengthAccepted",
      type = "int",
      defaultValue = "50"
   ), @NiagaraProperty(
      name = "segmentationSupported",
      type = "BBacnetSegmentation",
      defaultValue = "BBacnetSegmentation.noSegmentation"
   ), @NiagaraProperty(
      name = "vendorId",
      type = "int",
      defaultValue = "-1"
   ), @NiagaraProperty(
      name = "address",
      type = "BBacnetAddress",
      defaultValue = "BBacnetAddress.DEFAULT"
   ), @NiagaraProperty(
      name = "listSize",
      type = "int",
      defaultValue = "-1"
   ), @NiagaraProperty(
      name = "encoding",
      type = "BCharacterSetEncoding",
      defaultValue = "BCharacterSetEncoding.unknown"
   ), @NiagaraProperty(
      name = "servicesSupported",
      type = "BBacnetBitString",
      defaultValue = "BBacnetBitString.emptyBitString(BacnetBitStringUtil.getBitStringLength(\"BacnetServicesSupported\"))"
   ), @NiagaraProperty(
      name = "vendorName",
      type = "String",
      defaultValue = ""
   ), @NiagaraProperty(
      name = "modelName",
      type = "String",
      defaultValue = ""
   ), @NiagaraProperty(
      name = "protocolRevision",
      type = "int",
      defaultValue = "0"
   ), @NiagaraProperty(
      name = "firmwareRevision",
      type = "String",
      defaultValue = ""
   ), @NiagaraProperty(
      name = "applicationSoftwareVersion",
      type = "String",
      defaultValue = ""
   ), @NiagaraProperty(
      name = "duplicate",
      type = "boolean",
      defaultValue = "false"
   )})
public class BDiscoveryDevice extends BStruct {
   public static final Property deviceName = newProperty(0, "", null);
   public static final Property objectId = newProperty(0, BBacnetObjectIdentifier.DEFAULT, null);
   public static final Property maxApduLengthAccepted = newProperty(0, 50, null);
   public static final Property segmentationSupported = newProperty(0, BBacnetSegmentation.noSegmentation, null);
   public static final Property vendorId = newProperty(0, -1, null);
   public static final Property address = newProperty(0, BBacnetAddress.DEFAULT, null);
   public static final Property listSize = newProperty(0, -1, null);
   public static final Property encoding = newProperty(0, BCharacterSetEncoding.unknown, null);
   public static final Property servicesSupported = newProperty(
      0, BBacnetBitString.emptyBitString(BacnetBitStringUtil.getBitStringLength("BacnetServicesSupported")), null
   );
   public static final Property vendorName = newProperty(0, "", null);
   public static final Property modelName = newProperty(0, "", null);
   public static final Property protocolRevision = newProperty(0, 0, null);
   public static final Property firmwareRevision = newProperty(0, "", null);
   public static final Property applicationSoftwareVersion = newProperty(0, "", null);
   public static final Property duplicate = newProperty(0, false, null);
   public static final Type TYPE = Sys.loadType(BDiscoveryDevice.class);

   public String getDeviceName() {
      return this.getString(deviceName);
   }

   public void setDeviceName(String v) {
      this.setString(deviceName, v, null);
   }

   public BBacnetObjectIdentifier getObjectId() {
      return (BBacnetObjectIdentifier)this.get(objectId);
   }

   public void setObjectId(BBacnetObjectIdentifier v) {
      this.set(objectId, v, null);
   }

   public int getMaxApduLengthAccepted() {
      return this.getInt(maxApduLengthAccepted);
   }

   public void setMaxApduLengthAccepted(int v) {
      this.setInt(maxApduLengthAccepted, v, null);
   }

   public BBacnetSegmentation getSegmentationSupported() {
      return (BBacnetSegmentation)this.get(segmentationSupported);
   }

   public void setSegmentationSupported(BBacnetSegmentation v) {
      this.set(segmentationSupported, v, null);
   }

   public int getVendorId() {
      return this.getInt(vendorId);
   }

   public void setVendorId(int v) {
      this.setInt(vendorId, v, null);
   }

   public BBacnetAddress getAddress() {
      return (BBacnetAddress)this.get(address);
   }

   public void setAddress(BBacnetAddress v) {
      this.set(address, v, null);
   }

   public int getListSize() {
      return this.getInt(listSize);
   }

   public void setListSize(int v) {
      this.setInt(listSize, v, null);
   }

   public BCharacterSetEncoding getEncoding() {
      return (BCharacterSetEncoding)this.get(encoding);
   }

   public void setEncoding(BCharacterSetEncoding v) {
      this.set(encoding, v, null);
   }

   public BBacnetBitString getServicesSupported() {
      return (BBacnetBitString)this.get(servicesSupported);
   }

   public void setServicesSupported(BBacnetBitString v) {
      this.set(servicesSupported, v, null);
   }

   public String getVendorName() {
      return this.getString(vendorName);
   }

   public void setVendorName(String v) {
      this.setString(vendorName, v, null);
   }

   public String getModelName() {
      return this.getString(modelName);
   }

   public void setModelName(String v) {
      this.setString(modelName, v, null);
   }

   public int getProtocolRevision() {
      return this.getInt(protocolRevision);
   }

   public void setProtocolRevision(int v) {
      this.setInt(protocolRevision, v, null);
   }

   public String getFirmwareRevision() {
      return this.getString(firmwareRevision);
   }

   public void setFirmwareRevision(String v) {
      this.setString(firmwareRevision, v, null);
   }

   public String getApplicationSoftwareVersion() {
      return this.getString(applicationSoftwareVersion);
   }

   public void setApplicationSoftwareVersion(String v) {
      this.setString(applicationSoftwareVersion, v, null);
   }

   public boolean getDuplicate() {
      return this.getBoolean(duplicate);
   }

   public void setDuplicate(boolean v) {
      this.setBoolean(duplicate, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BDiscoveryDevice() {
   }

   public BDiscoveryDevice(
      String name,
      IAmRequest req,
      BBacnetAddress addr,
      int listSize,
      BCharacterSetEncoding encoding,
      BBacnetBitString servicesSupported,
      String vendorName,
      String modelName,
      int protocolRevision,
      String firmwareRevision,
      String applicationSoftwareVersion
   ) {
      this.setDeviceName(name);
      this.setObjectId(req.getObjectId());
      this.setMaxApduLengthAccepted(req.getMaxAPDULengthAccepted());
      this.setSegmentationSupported(req.getSegmentationSupported());
      this.setVendorId(req.getVendorId());
      this.getAddress().copyFrom(addr);
      this.setListSize(listSize);
      this.setEncoding(encoding);
      this.setServicesSupported(servicesSupported);
      this.setVendorName(vendorName);
      this.setModelName(modelName);
      this.setFirmwareRevision(firmwareRevision);
      this.setProtocolRevision(protocolRevision);
      this.setApplicationSoftwareVersion(applicationSoftwareVersion);
   }

   public Object makeKey() {
      return vendorName + ":" + modelName;
   }

   public String toString(Context cx) {
      StringBuilder sb = new StringBuilder();
      sb.append("name=")
         .append(this.getDeviceName())
         .append(" id=" + this.getObjectId())
         .append(" apdu=")
         .append(this.getMaxApduLengthAccepted())
         .append(" seg=" + this.getSegmentationSupported())
         .append(" vid=")
         .append(this.getVendorId())
         .append(" addr=" + this.getAddress())
         .append(" siz=")
         .append(this.getListSize())
         .append(" enc=" + this.getEncoding())
         .append(" svc=" + this.getServicesSupported())
         .append(" vNm=")
         .append(this.getVendorName())
         .append(" mNm=")
         .append(this.getModelName())
         .append(" fwR=")
         .append(this.getFirmwareRevision())
         .append(" asR=")
         .append(this.getApplicationSoftwareVersion());
      return sb.toString();
   }
}
