package com.tridium.bacnet.stack.link.sc.connection;

import javax.baja.bacnet.BacnetException;

public final class DuplicateVmacException extends BacnetException {
   public DuplicateVmacException(String s) {
      super(s, null);
   }
}
