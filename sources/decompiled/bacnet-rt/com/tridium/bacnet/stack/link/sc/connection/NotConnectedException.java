package com.tridium.bacnet.stack.link.sc.connection;

import javax.baja.bacnet.BacnetException;

public final class NotConnectedException extends BacnetException {
   public NotConnectedException(String s) {
      super(s, null);
   }
}
