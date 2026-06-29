package com.tridium.bacnet.services.confirmed;

import com.tridium.bacnet.services.BacnetSimpleAck;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;

public class DeleteObjectAck extends BacnetSimpleAck {
   private BBacnetObjectIdentifier objectId;

   public DeleteObjectAck() {
      super(11);
   }

   public DeleteObjectAck(BBacnetObjectIdentifier objectId) {
      this();
      this.objectId = objectId;
   }
}
