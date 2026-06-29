package javax.baja.bacnet.io;

import javax.baja.bacnet.BacnetException;

public class AsnException extends BacnetException {
   public AsnException(String detailMessage) {
      super(detailMessage);
   }

   public String toString() {
      return lex.getText("AsnException.asn") + ":" + this.getMessage();
   }
}
