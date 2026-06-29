package javax.baja.lonworks.londata;

import javax.baja.sys.BajaRuntimeException;

public class InvalidTypeException extends BajaRuntimeException {
   public InvalidTypeException(String msg) {
      super(msg);
   }

   public InvalidTypeException() {
   }
}
