package javax.baja.bacnet.io;

public class OutOfRangeException extends AsnException {
   public OutOfRangeException(String detailMessage) {
      super(detailMessage);
   }

   @Override
   public String toString() {
      return lex.getText("AsnException.asn") + ":" + this.getMessage();
   }
}
