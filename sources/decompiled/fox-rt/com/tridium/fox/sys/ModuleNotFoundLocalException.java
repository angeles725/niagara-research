package com.tridium.fox.sys;

import javax.baja.sys.LocalizableRuntimeException;

public class ModuleNotFoundLocalException extends LocalizableRuntimeException {
   public ModuleNotFoundLocalException(String moduleName, Throwable cause) {
      super("fox", "ModuleNotFoundLocalException", new Object[]{moduleName}, cause);
   }
}
