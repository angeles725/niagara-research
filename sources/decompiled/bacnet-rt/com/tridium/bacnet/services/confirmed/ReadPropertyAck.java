package com.tridium.bacnet.services.confirmed;

import com.tridium.bacnet.asn.AsnInputStream;
import com.tridium.bacnet.asn.AsnOutputStream;
import com.tridium.bacnet.services.BacnetComplexAck;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.enums.BBacnetPropertyIdentifier;
import javax.baja.bacnet.io.AsnException;
import javax.baja.sys.BEnum;

public class ReadPropertyAck extends BacnetComplexAck {
   public static final int OBJECT_ID_TAG = 0;
   public static final int PROPERTY_ID_TAG = 1;
   public static final int PROPERTY_ARRAY_INDEX_TAG = 2;
   public static final int PROPERTY_VALUE_TAG = 3;
   private BBacnetObjectIdentifier objectId;
   private int propertyId;
   private int propertyArrayIndex;
   private byte[] encodedValue;

   public ReadPropertyAck() {
      super(12);
   }

   public ReadPropertyAck(BBacnetObjectIdentifier objectId, BEnum propertyId, byte[] encodedValue) {
      this(objectId, propertyId.getOrdinal(), -1, encodedValue);
   }

   public ReadPropertyAck(BBacnetObjectIdentifier objectId, int propertyId, byte[] encodedValue) {
      this(objectId, propertyId, -1, encodedValue);
   }

   public ReadPropertyAck(BBacnetObjectIdentifier objectId, BEnum propertyId, int propertyArrayIndex, byte[] encodedValue) {
      this(objectId, propertyId.getOrdinal(), propertyArrayIndex, encodedValue);
   }

   public ReadPropertyAck(BBacnetObjectIdentifier objectId, int propertyId, int propertyArrayIndex, byte[] encodedValue) {
      super(12);
      this.objectId = objectId;
      this.propertyId = propertyId;
      this.propertyArrayIndex = propertyArrayIndex;
      this.encodedValue = encodedValue;
   }

   public ReadPropertyAck(int serviceChoice, AsnInputStream inputStream) throws AsnException {
      super(serviceChoice);
      this.readEncoded(inputStream);
   }

   public BBacnetObjectIdentifier getObjectId() {
      return this.objectId;
   }

   public void setObjectId(BBacnetObjectIdentifier objectId) {
      this.objectId = objectId;
   }

   public byte[] getEncodedValue() {
      return this.encodedValue;
   }

   @Override
   public void writeEncoded(AsnOutputStream outputStream) {
      outputStream.writeObjectIdentifier(0, this.objectId);
      outputStream.writeEnumerated(1, this.propertyId);
      if (this.propertyArrayIndex != -1) {
         outputStream.writeUnsignedInteger(2, this.propertyArrayIndex);
      }

      outputStream.writeEncodedValue(3, this.encodedValue);
   }

   @Override
   public void readEncoded(AsnInputStream inputStream) throws AsnException {
      this.objectId = inputStream.readObjectIdentifier(0);
      this.propertyId = inputStream.readEnumerated(1);
      inputStream.peekTag();
      if (inputStream.isValueTag(2)) {
         this.propertyArrayIndex = inputStream.readUnsignedInt(2);
      } else {
         this.propertyArrayIndex = -1;
      }

      this.encodedValue = inputStream.readEncodedValue(3);
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder("ReadPropertyACK: ");
      sb.append("\n  " + this.objectId);
      sb.append("\n  " + BBacnetPropertyIdentifier.tag(this.propertyId));
      if (this.propertyArrayIndex != -1) {
         sb.append("[" + this.propertyArrayIndex + "]");
      }

      return sb.toString();
   }
}
