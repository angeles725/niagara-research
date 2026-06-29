package com.tridium.bacnet.stack.network;

import com.tridium.bacnet.stack.BacnetStackException;

public class UnsupportedNetworkMsgException extends BacnetStackException {
   public UnsupportedNetworkMsgException(int msgCode) {
      super("Unsupported Network Layer Message: type=" + msgCode);
   }
}
