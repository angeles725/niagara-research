package com.tridium.bacnet.services.confirmed;

import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;

public class RemoveListElementRequest extends ListElementRequest {
   public RemoveListElementRequest() {
      this(null, -1, -1, null);
   }

   public RemoveListElementRequest(BBacnetObjectIdentifier objectId, int propertyId, int propertyArrayIndex, byte[] listOfElements) {
      super(9, objectId, propertyId, propertyArrayIndex, listOfElements);
   }
}
