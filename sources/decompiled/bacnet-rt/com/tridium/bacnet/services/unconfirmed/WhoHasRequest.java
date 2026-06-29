package com.tridium.bacnet.services.unconfirmed;

import com.tridium.bacnet.asn.AsnInputStream;
import com.tridium.bacnet.asn.AsnOutputStream;
import com.tridium.bacnet.services.BacnetUnconfirmedRequest;
import javax.baja.bacnet.BBacnetNetwork;
import javax.baja.bacnet.BacnetUnconfirmedServiceChoice;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.enums.BCharacterSetEncoding;
import javax.baja.bacnet.io.AsnException;

public class WhoHasRequest extends BacnetUnconfirmedRequest {
   private static final int LIMITS_NOT_USED = -1;
   private static final int DEVICE_INSTANCE_LOW_RANGE_TAG = 0;
   private static final int DEVICE_INSTANCE_HIGH_RANGE_TAG = 1;
   private static final int OBJECT_IDENTIFIER_TAG = 2;
   private static final int OBJECT_NAME_TAG = 3;
   private int deviceInstanceRangeLowLimit = -1;
   private int deviceInstanceRangeHighLimit = -1;
   private BBacnetObjectIdentifier objectId = null;
   private String objectName = null;
   private BCharacterSetEncoding encoding = null;

   public WhoHasRequest() {
      this(-1, -1, null, null);
   }

   public WhoHasRequest(int deviceInstanceRangeLowLimit, int deviceInstanceRangeHighLimit, BBacnetObjectIdentifier objectId) {
      this(deviceInstanceRangeLowLimit, deviceInstanceRangeHighLimit, objectId, null, null);
   }

   public WhoHasRequest(int deviceInstanceRangeLowLimit, int deviceInstanceRangeHighLimit, String objectName, BCharacterSetEncoding encoding) {
      this(deviceInstanceRangeLowLimit, deviceInstanceRangeHighLimit, null, objectName, encoding);
   }

   public WhoHasRequest(String objectName, BCharacterSetEncoding encoding) {
      this(-1, -1, null, objectName, encoding);
   }

   public WhoHasRequest(BBacnetObjectIdentifier objectId) {
      this(-1, -1, objectId, null, null);
   }

   private WhoHasRequest(
      int deviceInstanceRangeLowLimit, int deviceInstanceRangeHighLimit, BBacnetObjectIdentifier objectId, String objectName, BCharacterSetEncoding encoding
   ) {
      super(7);
      this.deviceInstanceRangeLowLimit = deviceInstanceRangeLowLimit;
      this.deviceInstanceRangeHighLimit = deviceInstanceRangeHighLimit;
      this.objectId = objectId;
      this.objectName = objectName;
      this.encoding = encoding;
   }

   public int getDeviceInstanceRangeLowLimit() {
      return this.deviceInstanceRangeLowLimit;
   }

   public int getDeviceInstanceRangeHighLimit() {
      return this.deviceInstanceRangeHighLimit;
   }

   public boolean useLimits() {
      return this.deviceInstanceRangeLowLimit != -1;
   }

   public String getObjectName() {
      return this.objectName;
   }

   public BBacnetObjectIdentifier getObjectId() {
      return this.objectId;
   }

   public BCharacterSetEncoding getEncoding() {
      return this.encoding;
   }

   @Override
   public void writeEncoded(AsnOutputStream outputStream) {
      if (this.deviceInstanceRangeLowLimit != -1) {
         outputStream.writeUnsignedInteger(0, this.deviceInstanceRangeLowLimit);
         outputStream.writeUnsignedInteger(1, this.deviceInstanceRangeHighLimit);
      }

      if (this.objectId != null) {
         outputStream.writeObjectIdentifier(2, this.objectId);
      } else {
         outputStream.writeCharacterString(3, this.objectName, this.encoding);
      }
   }

   @Override
   public void readEncoded(AsnInputStream inputStream) throws AsnException {
      if (inputStream.peekTag() == 0) {
         this.deviceInstanceRangeLowLimit = inputStream.readUnsignedInt(0);
         this.deviceInstanceRangeHighLimit = inputStream.readUnsignedInt(1);
      } else {
         this.deviceInstanceRangeLowLimit = -1;
         this.deviceInstanceRangeHighLimit = -1;
      }

      if (inputStream.peekTag() == 2) {
         this.objectId = inputStream.readObjectIdentifier(2);
         this.encoding = BBacnetNetwork.localDevice().getCharacterSet();
      } else {
         this.encoding = inputStream.peekEncoding(3);
         this.objectName = inputStream.readCharacterString(3);
      }
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder(256);
      sb.append(BacnetUnconfirmedServiceChoice.TAGS[7]);
      if (this.deviceInstanceRangeLowLimit != -1) {
         sb.append("\n  deviceInstanceRangeLowLimit " + this.deviceInstanceRangeLowLimit);
         sb.append("\n  deviceInstanceRangeHighLimit " + this.deviceInstanceRangeHighLimit);
      } else {
         sb.append("\n  ALL DEVICES");
      }

      if (this.objectId != null) {
         sb.append("\n  objectId " + this.objectId);
      } else {
         sb.append("\n  objectName " + this.objectName);
         sb.append("\n  encoding " + this.encoding.getTag());
      }

      return sb.toString();
   }
}
