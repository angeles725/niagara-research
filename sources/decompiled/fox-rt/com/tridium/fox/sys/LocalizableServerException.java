package com.tridium.fox.sys;

import javax.baja.sys.LocalizableRuntimeException;

public class LocalizableServerException extends LocalizableRuntimeException {
   public LocalizableServerException(String lexiconModule, String lexiconKey, Object[] lexiconArgs) {
      super(lexiconModule, lexiconKey, lexiconArgs, null);
   }
}
