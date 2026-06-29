package com.tridium.bacnet.stack.network;

import com.tridium.bacnet.stack.BacnetStackException;

public class InvalidNetworkMsgException extends BacnetStackException {
   public InvalidNetworkMsgException() {
      super("Invalid Network Layer Message");
   }
}
