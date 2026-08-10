package com.tridium.program.ui.module;

import javax.baja.sys.BajaRuntimeException;

public class BuildException extends BajaRuntimeException {
   public BuildException() {
   }

   public BuildException(Throwable cause) {
      super(cause);
   }

   public BuildException(String msg) {
      super(msg);
   }

   public BuildException(String msg, Throwable cause) {
      super(msg, cause);
   }
}
