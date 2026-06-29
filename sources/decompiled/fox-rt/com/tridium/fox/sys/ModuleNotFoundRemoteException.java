package com.tridium.fox.sys;

import javax.baja.sys.LocalizableRuntimeException;

public class ModuleNotFoundRemoteException extends LocalizableRuntimeException {
   public ModuleNotFoundRemoteException(String moduleName) {
      super("fox", "ModuleNotFoundRemoteException", new Object[]{moduleName});
   }
}
