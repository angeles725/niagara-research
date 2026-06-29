package com.tridium.bacnet.stack.network;

import com.tridium.bacnet.stack.BacnetStackException;

public class UnsupportedProtocolVersionException extends BacnetStackException {
   public UnsupportedProtocolVersionException(int versionCode) {
      super("Unsupported Protocol Version: " + versionCode);
   }
}
