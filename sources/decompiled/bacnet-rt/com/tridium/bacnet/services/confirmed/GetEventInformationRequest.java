package com.tridium.bacnet.services.confirmed;

import com.tridium.bacnet.asn.AsnInputStream;
import com.tridium.bacnet.asn.AsnOutputStream;
import com.tridium.bacnet.services.BacnetConfirmedRequest;
import com.tridium.bacnet.services.BacnetServicePrimitive;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.io.RejectException;

public class GetEventInformationRequest extends BacnetConfirmedRequest {
   public static final int LAST_RECEIVED_OBJECT_ID_TAG = 0;
   private BBacnetObjectIdentifier lastReceivedObjectId;

   public GetEventInformationRequest() {
      super(29);
   }

   public GetEventInformationRequest(BBacnetObjectIdentifier lastReceivedObjectId) {
      super(29);
      this.lastReceivedObjectId = lastReceivedObjectId;
   }

   public BBacnetObjectIdentifier getLastReceivedObjectId() {
      return this.lastReceivedObjectId;
   }

   public void setLastReceivedObjectId(BBacnetObjectIdentifier lastReceivedObjectId) {
      this.lastReceivedObjectId = lastReceivedObjectId;
   }

   @Override
   public void writeEncoded(AsnOutputStream out) {
      if (this.lastReceivedObjectId != null) {
         out.writeObjectIdentifier(0, this.lastReceivedObjectId);
      }
   }

   @Override
   public void readEncoded(AsnInputStream in) throws AsnException, RejectException {
      if (in.peekTag() != -1) {
         this.lastReceivedObjectId = in.readObjectIdentifier(0);
      }
   }

   @Override
   public BacnetServicePrimitive doParseAck(int serviceChoice, AsnInputStream inputStream) throws AsnException {
      return new GetEventInformationAck(serviceChoice, inputStream);
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder("GetEventInformationRequest: ");
      sb.append("\n  " + this.lastReceivedObjectId);
      return sb.toString();
   }
}
