package javax.baja.bacnet.io;

public class DataTypeNotSupportedException extends AsnException {
   public DataTypeNotSupportedException(String detailMessage) {
      super(detailMessage);
   }

   @Override
   public String toString() {
      return lex.getText("AsnException.asn") + ":" + this.getMessage();
   }
}
