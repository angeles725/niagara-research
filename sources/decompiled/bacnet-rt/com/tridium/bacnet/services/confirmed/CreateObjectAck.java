package com.tridium.bacnet.services.confirmed;

import com.tridium.bacnet.asn.AsnInputStream;
import com.tridium.bacnet.asn.AsnOutputStream;
import com.tridium.bacnet.services.BacnetComplexAck;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.io.AsnException;

public class CreateObjectAck extends BacnetComplexAck {
   private BBacnetObjectIdentifier objectId;

   public CreateObjectAck() {
      super(10);
   }

   public CreateObjectAck(BBacnetObjectIdentifier objectId) {
      super(10);
      this.objectId = objectId;
   }

   public CreateObjectAck(int serviceChoice, AsnInputStream inputStream) throws AsnException {
      super(serviceChoice);
      this.readEncoded(inputStream);
   }

   public final BBacnetObjectIdentifier getObjectId() {
      return this.objectId;
   }

   public final void setObjectId(BBacnetObjectIdentifier objectId) {
      this.objectId = objectId;
   }

   @Override
   public void readEncoded(AsnInputStream in) throws AsnException {
      this.objectId = in.readObjectIdentifier();
   }

   @Override
   public void writeEncoded(AsnOutputStream out) {
      out.writeObjectIdentifier(this.objectId);
   }
}
