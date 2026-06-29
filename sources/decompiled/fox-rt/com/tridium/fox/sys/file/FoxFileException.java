package com.tridium.fox.sys.file;

import com.tridium.fox.session.ServerException;
import javax.baja.sys.BajaRuntimeException;
import javax.baja.sys.Context;

public class FoxFileException extends BajaRuntimeException {
   public FoxFileException(String msg, Throwable cause) {
      super(msg, cause);
   }

   public FoxFileException(String msg) {
      super(msg);
   }

   public String toString(Context context) {
      Throwable cause = this.getCause();
      return cause instanceof ServerException && cause != this ? super.toString(context) + " \n\n" + cause.getLocalizedMessage() : super.toString(context);
   }
}
