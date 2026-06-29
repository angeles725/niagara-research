package javax.baja.bacnet.io;

import javax.baja.sys.LocalizableRuntimeException;

public class IllegalActionInitiationError extends LocalizableRuntimeException {
   public IllegalActionInitiationError(String lexiconModule, String lexiconKey, Object[] lexiconArgs) {
      super(lexiconModule, lexiconKey, lexiconArgs, null);
   }
}
