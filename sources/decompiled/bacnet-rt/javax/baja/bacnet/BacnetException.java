package javax.baja.bacnet;

import javax.baja.sys.BajaException;
import javax.baja.util.Lexicon;

public class BacnetException extends BajaException {
   protected static Lexicon lex = Lexicon.make("bacnet");

   public BacnetException(String msg, Throwable cause) {
      super(msg, cause);
   }

   public BacnetException(Throwable cause) {
      super(cause);
   }

   public BacnetException(String msg) {
      super(msg);
   }

   public BacnetException() {
   }
}
