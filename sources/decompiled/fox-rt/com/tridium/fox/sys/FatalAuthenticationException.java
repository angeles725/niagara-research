package com.tridium.fox.sys;

import javax.baja.sys.LocalizableRuntimeException;

public class FatalAuthenticationException extends LocalizableRuntimeException {
   public FatalAuthenticationException(String msg) {
      super("fox", "FatalAuthenticationException", new Object[]{msg}, null);
   }
}
