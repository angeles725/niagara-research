package com.tridium.bacnet.services.confirmed;

import com.tridium.bacnet.asn.AsnInputStream;
import com.tridium.bacnet.asn.AsnOutputStream;
import com.tridium.bacnet.services.BacnetConfirmedRequest;
import com.tridium.bacnet.services.BacnetServicePrimitive;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.enums.BBacnetPropertyIdentifier;
import javax.baja.bacnet.io.AsnException;
import javax.baja.sys.BEnum;

public class ReadPropertyRequest extends BacnetConfirmedRequest {
   public static final int OBJECT_ID_TAG = 0;
   public static final int PROPERTY_ID_TAG = 1;
   public static final int PROPERTY_ARRAY_INDEX_TAG = 2;
   private BBacnetObjectIdentifier objectId;
   private int propertyId;
   private int propertyArrayIndex;

   public ReadPropertyRequest() {
      this(null, -1, -1);
   }

   public ReadPropertyRequest(BBacnetObjectIdentifier objectId, int propertyId) {
      this(objectId, propertyId, -1);
   }

   public ReadPropertyRequest(BBacnetObjectIdentifier objectId, int propertyId, int propertyArrayIndex) {
      super(12);
      this.objectId = objectId;
      this.propertyId = propertyId;
      this.propertyArrayIndex = propertyArrayIndex;
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

   @Override
   public void writeEncoded(AsnOutputStream outputStream) {
      outputStream.writeObjectIdentifier(0, this.objectId);
      outputStream.writeEnumerated(1, this.propertyId);
      if (this.propertyArrayIndex != -1) {
         outputStream.writeUnsignedInteger(2, this.propertyArrayIndex);
      }
   }

   @Override
   public void readEncoded(AsnInputStream inputStream) throws AsnException {
      this.objectId = inputStream.readObjectIdentifier(0);
      this.propertyId = inputStream.readEnumerated(1);
      if (inputStream.peekTag() != -1) {
         if (!inputStream.isValueTag(2)) {
            throw new AsnException("Invalid tag: " + inputStream.peekTag());
         }

         this.propertyArrayIndex = inputStream.readUnsignedInt(2);
      }

      if (inputStream.peekTag() != -1) {
         throw new AsnException("Invalid tag: " + inputStream.peekTag());
      }
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder("ReadPropertyRequest: ");
      sb.append("\n  " + this.objectId);
      sb.append("\n  " + BBacnetPropertyIdentifier.tag(this.propertyId));
      if (this.propertyArrayIndex != -1) {
         sb.append("[" + this.propertyArrayIndex + "]");
      }

      return sb.toString();
   }

   @Override
   public BacnetServicePrimitive doParseAck(int serviceChoice, AsnInputStream inputStream) throws AsnException {
      return new ReadPropertyAck(serviceChoice, inputStream);
   }
}
