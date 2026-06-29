package com.tridium.bacnet.stack.transport;

import com.tridium.bacnet.stack.BacnetStackException;

public class InvalidApduLengthException extends BacnetStackException {
   public InvalidApduLengthException(int expectedLength, int actualLength) {
      super("Invalid APDU length: exp " + expectedLength + " != act " + actualLength);
   }
}
