package javax.baja.bacnet.util;

import javax.baja.bacnet.io.ErrorType;
import javax.baja.sys.BEnumRange;

public class EnumRangeWrapper {
   private BEnumRange enumRange;
   private ErrorType errorType;

   public BEnumRange getEnumRange() {
      return this.enumRange;
   }

   public void setEnumRange(BEnumRange enumRange) {
      this.enumRange = enumRange;
   }

   public ErrorType getErrorType() {
      return this.errorType;
   }

   public void markError(ErrorType errorType) {
      this.errorType = errorType;
   }

   public static EnumRangeWrapper make(BEnumRange enumRange, ErrorType errorType) {
      EnumRangeWrapper enumRangeWrapper = new EnumRangeWrapper();
      enumRangeWrapper.setEnumRange(enumRange);
      enumRangeWrapper.markError(errorType);
      return enumRangeWrapper;
   }
}
