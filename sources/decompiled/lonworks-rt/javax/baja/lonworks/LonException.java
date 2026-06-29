package javax.baja.lonworks;

import javax.baja.sys.BajaException;

public class LonException extends BajaException {
   public LonException(String s) {
      super(s, null);
   }

   public LonException(String s, Throwable cause) {
      super(s, cause);
   }
}
