package com.tridium.bacnet.stack.transport;

import com.tridium.bacnet.stack.BacnetStackException;

public class InvalidApduTypeException extends BacnetStackException {
   public InvalidApduTypeException(int pduType) {
      super("Invalid APDU type: " + pduType);
   }

   public InvalidApduTypeException(int expectedPduType, int actualPduType) {
      super("Invalid APDU type: exp " + expectedPduType + " != act " + actualPduType);
   }

   public InvalidApduTypeException(String detailMessage, int expectedPduType, int actualPduType) {
      super("Invalid APDU type: " + detailMessage + " exp " + expectedPduType + " != act " + actualPduType);
   }
}
