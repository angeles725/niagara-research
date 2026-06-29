package javax.baja.bacnet.io;

public class AsnDataTypeNotSupportedException extends DataTypeNotSupportedException {
   private int asnType;

   public AsnDataTypeNotSupportedException(String detailMessage) {
      super(detailMessage);
   }

   public AsnDataTypeNotSupportedException(int asnType, String detailMessage) {
      this(detailMessage);
      this.asnType = asnType;
   }

   public int getAsnType() {
      return this.asnType;
   }
}
