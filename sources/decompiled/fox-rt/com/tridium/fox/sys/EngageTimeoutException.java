package com.tridium.fox.sys;

import javax.baja.sys.BajaRuntimeException;

public class EngageTimeoutException extends BajaRuntimeException {
   public EngageTimeoutException(String msg) {
      super(msg);
   }

   public EngageTimeoutException() {
   }
}
