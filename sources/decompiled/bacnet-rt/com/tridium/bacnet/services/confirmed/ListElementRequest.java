package com.tridium.bacnet.services.confirmed;

import com.tridium.bacnet.asn.AsnInputStream;
import com.tridium.bacnet.asn.AsnOutputStream;
import com.tridium.bacnet.services.BacnetConfirmedRequest;
import com.tridium.bacnet.services.BacnetError;
import com.tridium.bacnet.services.error.NChangeListError;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.enums.BBacnetPropertyIdentifier;
import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.io.RejectException;
import javax.baja.nre.util.ByteArrayUtil;

public abstract class ListElementRequest extends BacnetConfirmedRequest {
   public static final int OBJECT_ID_TAG = 0;
   public static final int PROPERTY_ID_TAG = 1;
   public static final int PROPERTY_ARRAY_INDEX_TAG = 2;
   public static final int LIST_OF_ELEMENTS_TAG = 3;
   private BBacnetObjectIdentifier objectId;
   private int propertyId;
   private int propertyArrayIndex;
   private byte[] listOfElements;

   protected ListElementRequest(int serviceChoice, BBacnetObjectIdentifier objectId, int propertyId, int propertyArrayIndex, byte[] listOfElements) {
      super(serviceChoice);
      this.objectId = objectId;
      this.propertyId = propertyId;
      this.propertyArrayIndex = propertyArrayIndex;
      this.listOfElements = listOfElements;
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

   public byte[] getListOfElements() {
      return this.listOfElements;
   }

   public void setListOfElements(byte[] list) {
      this.listOfElements = list;
   }

   @Override
   public void writeEncoded(AsnOutputStream outputStream) {
      outputStream.writeObjectIdentifier(0, this.objectId);
      outputStream.writeEnumerated(1, this.propertyId);
      if (this.propertyArrayIndex != -1) {
         outputStream.writeUnsignedInteger(2, this.propertyArrayIndex);
      }

      outputStream.writeEncodedValue(3, this.listOfElements);
   }

   @Override
   public void readEncoded(AsnInputStream inputStream) throws AsnException, RejectException {
      this.objectId = inputStream.readObjectIdentifier(0);
      this.propertyId = inputStream.readEnumerated(1);
      inputStream.peekTag();
      if (inputStream.isValueTag(2)) {
         this.propertyArrayIndex = inputStream.readUnsignedInt(2);
      } else {
         this.propertyArrayIndex = -1;
      }

      this.listOfElements = inputStream.readEncodedValue(3);
   }

   @Override
   public BacnetError doParseError(int errorChoice, byte[] encodedError) throws AsnException {
      return new NChangeListError(errorChoice, encodedError);
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder(TAGS[this.getServiceChoice()] + ":");
      sb.append("\n  " + this.objectId);
      sb.append("\n  " + BBacnetPropertyIdentifier.tag(this.propertyId));
      if (this.propertyArrayIndex != -1) {
         sb.append("[" + this.propertyArrayIndex + "]");
      }

      sb.append("\n  " + ByteArrayUtil.toHexString(this.listOfElements));
      return sb.toString();
   }
}
