package com.tridium.bacnet.stack.link.sc;

import javax.baja.bacnet.BacnetException;

public final class NoConnectionException extends BacnetException {
   public NoConnectionException(String msg) {
      super(msg);
   }
}
