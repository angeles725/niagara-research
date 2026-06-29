package com.tridium.bacnet.services.confirmed;

import com.tridium.bacnet.asn.AsnInputStream;
import com.tridium.bacnet.asn.AsnOutputStream;
import com.tridium.bacnet.services.BacnetConfirmedRequest;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.io.RejectException;

public class DeleteObjectRequest extends BacnetConfirmedRequest {
   private BBacnetObjectIdentifier objectId = null;

   public DeleteObjectRequest() {
      super(11);
   }

   public DeleteObjectRequest(BBacnetObjectIdentifier objectId) {
      super(11);
      this.objectId = objectId;
   }

   @Override
   public void readEncoded(AsnInputStream in) throws AsnException, RejectException {
      this.objectId = in.readObjectIdentifier();
   }

   public BBacnetObjectIdentifier getObjectId() {
      return this.objectId;
   }

   @Override
   public void writeEncoded(AsnOutputStream out) {
      out.writeObjectIdentifier(this.objectId);
   }
}
