package com.tridium.bacnet.services.confirmed;

import com.tridium.bacnet.asn.AsnInputStream;
import com.tridium.bacnet.asn.AsnOutputStream;
import com.tridium.bacnet.services.BacnetConfirmedRequest;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.enums.BBacnetPropertyIdentifier;
import javax.baja.bacnet.io.AsnException;
import javax.baja.nre.util.ByteArrayUtil;
import javax.baja.sys.BEnum;

public class WritePropertyRequest extends BacnetConfirmedRequest {
   public static final int OBJECT_ID_TAG = 0;
   public static final int PROPERTY_ID_TAG = 1;
   public static final int PROPERTY_ARRAY_INDEX_TAG = 2;
   public static final int PROPERTY_VALUE_TAG = 3;
   public static final int PRIORITY_TAG = 4;
   private BBacnetObjectIdentifier objectId;
   private int propertyId;
   private int propertyArrayIndex;
   private int priority;
   private byte[] encodedValue;

   public WritePropertyRequest() {
      this(null, -1, -1, null, -1);
   }

   public WritePropertyRequest(BBacnetObjectIdentifier objectId, BEnum propertyId, byte[] encodedValue) {
      this(objectId, propertyId.getOrdinal(), -1, encodedValue, -1);
   }

   public WritePropertyRequest(BBacnetObjectIdentifier objectId, BEnum propertyId, int propertyArrayIndex, byte[] encodedValue) {
      this(objectId, propertyId.getOrdinal(), propertyArrayIndex, encodedValue, -1);
   }

   public WritePropertyRequest(BBacnetObjectIdentifier objectId, BEnum propertyId, int propertyArrayIndex, byte[] encodedValue, int priority) {
      this(objectId, propertyId.getOrdinal(), propertyArrayIndex, encodedValue, priority);
   }

   public WritePropertyRequest(BBacnetObjectIdentifier objectId, int propertyId, byte[] encodedValue) {
      this(objectId, propertyId, -1, encodedValue, -1);
   }

   public WritePropertyRequest(BBacnetObjectIdentifier objectId, int propertyId, int propertyArrayIndex, byte[] encodedValue) {
      this(objectId, propertyId, propertyArrayIndex, encodedValue, -1);
   }

   public WritePropertyRequest(BBacnetObjectIdentifier objectId, int propertyId, int propertyArrayIndex, byte[] encodedValue, int priority) {
      super(15);
      this.objectId = objectId;
      this.propertyId = propertyId;
      this.propertyArrayIndex = propertyArrayIndex;
      this.encodedValue = encodedValue;
      this.priority = priority;
   }

   public BBacnetObjectIdentifier getObjectId() {
      return this.objectId;
   }

   public void setObjectId(BBacnetObjectIdentifier objectId) {
      this.objectId = objectId;
   }

   public int getPropertyId() {
      return this.propertyId;
   }

   public void setPropertyId(BEnum propertyId) {
      this.propertyId = propertyId.getOrdinal();
   }

   public void setPropertyId(int propertyId) {
      this.propertyId = propertyId;
   }

   public boolean isPropertyArrayIndexUsed() {
      return this.propertyArrayIndex != -1;
   }

   public int getPropertyArrayIndex() {
      return this.propertyArrayIndex;
   }

   public void setPropertyArrayIndex(int propertyArrayIndex) {
      this.propertyArrayIndex = propertyArrayIndex;
   }

   public byte[] getEncodedValue() {
      return this.encodedValue;
   }

   public int getPriority() {
      return this.priority;
   }

   @Override
   public void writeEncoded(AsnOutputStream outputStream) {
      outputStream.writeObjectIdentifier(0, this.objectId);
      outputStream.writeEnumerated(1, this.propertyId);
      if (this.propertyArrayIndex != -1) {
         outputStream.writeUnsignedInteger(2, this.propertyArrayIndex);
      }

      outputStream.writeEncodedValue(3, this.encodedValue);
      if (this.priority != -1) {
         outputStream.writeUnsignedInteger(4, this.priority);
      }
   }

   @Override
   public void readEncoded(AsnInputStream inputStream) throws AsnException {
      this.propertyArrayIndex = -1;
      this.priority = -1;
      this.objectId = inputStream.readObjectIdentifier(0);
      this.propertyId = inputStream.readEnumerated(1);
      inputStream.peekTag();
      if (inputStream.isValueTag(2)) {
         this.propertyArrayIndex = inputStream.readUnsignedInt(2);
      }

      this.encodedValue = inputStream.readEncodedValue(3);
      inputStream.peekTag();
      if (inputStream.isValueTag(4)) {
         this.priority = inputStream.readUnsignedInt(4);
      }
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder("WritePropertyRequest: ");
      sb.append("\n  " + this.objectId);
      sb.append("\n  " + BBacnetPropertyIdentifier.tag(this.propertyId));
      if (this.propertyArrayIndex != -1) {
         sb.append("[" + this.propertyArrayIndex + "]");
      }

      sb.append("\n  encodedValue=" + ByteArrayUtil.toHexString(this.encodedValue));
      if (this.priority != -1) {
         sb.append("\n  @" + this.priority);
      }

      return sb.toString();
   }
}
