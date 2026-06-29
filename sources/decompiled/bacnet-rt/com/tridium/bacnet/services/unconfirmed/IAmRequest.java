package com.tridium.bacnet.services.unconfirmed;

import com.tridium.bacnet.asn.AsnInputStream;
import com.tridium.bacnet.asn.AsnOutputStream;
import com.tridium.bacnet.services.BacnetUnconfirmedRequest;
import javax.baja.bacnet.BacnetUnconfirmedServiceChoice;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.enums.BBacnetSegmentation;
import javax.baja.bacnet.io.AsnException;

public class IAmRequest extends BacnetUnconfirmedRequest {
   private BBacnetObjectIdentifier objectId;
   private int maxApduLengthAccepted;
   private BBacnetSegmentation segmentationSupported;
   private int vendorId;

   public IAmRequest() {
      this(null, 0, BBacnetSegmentation.noSegmentation, 0);
   }

   public IAmRequest(BBacnetObjectIdentifier objectId, int maxApduLengthAccepted, BBacnetSegmentation segmentationSupported, int vendorId) {
      super(0);
      this.objectId = objectId;
      this.maxApduLengthAccepted = maxApduLengthAccepted;
      this.segmentationSupported = segmentationSupported;
      this.vendorId = vendorId;
   }

   public BBacnetObjectIdentifier getObjectId() {
      return this.objectId;
   }

   public void setObjectId(BBacnetObjectIdentifier objectId) {
      this.objectId = objectId;
   }

   public int getMaxAPDULengthAccepted() {
      return this.maxApduLengthAccepted;
   }

   public BBacnetSegmentation getSegmentationSupported() {
      return this.segmentationSupported;
   }

   public int getVendorId() {
      return this.vendorId;
   }

   @Override
   public void writeEncoded(AsnOutputStream outputStream) {
      outputStream.writeObjectIdentifier(this.objectId);
      outputStream.writeUnsignedInteger(this.maxApduLengthAccepted);
      outputStream.writeEnumerated(this.segmentationSupported.getOrdinal());
      outputStream.writeUnsignedInteger(this.vendorId);
   }

   @Override
   public void readEncoded(AsnInputStream inputStream) throws AsnException {
      this.objectId = inputStream.readObjectIdentifier();
      this.maxApduLengthAccepted = inputStream.readUnsignedInt();
      this.segmentationSupported = BBacnetSegmentation.make(inputStream.readEnumerated());
      this.vendorId = inputStream.readUnsignedInt();
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder(BacnetUnconfirmedServiceChoice.TAGS[0]);
      sb.append("\n  objectId " + this.objectId);
      sb.append("\n  maxApduLengthAccepted " + this.maxApduLengthAccepted);
      sb.append("\n  segmentationSupported " + this.segmentationSupported);
      sb.append("\n  vendorId " + this.vendorId);
      return sb.toString();
   }
}
