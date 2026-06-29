package com.tridium.bacnet.services.confirmed;

import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;

public class AddListElementRequest extends ListElementRequest {
   public AddListElementRequest() {
      this(null, -1, -1, null);
   }

   public AddListElementRequest(BBacnetObjectIdentifier objectId, int propertyId, int propertyArrayIndex, byte[] listOfElements) {
      super(8, objectId, propertyId, propertyArrayIndex, listOfElements);
   }
}
