package javax.baja.bacnet.io;

import javax.baja.bacnet.BacnetException;

public class ErrorException extends BacnetException {
   private ErrorType errorType;
   private Object[] errorParameters = null;

   public ErrorException(ErrorType errorType) {
      super(errorType.toString());
      this.errorType = errorType;
   }

   public ErrorException(ErrorType errorType, Object[] errorParameters) {
      super(errorType.toString());
      this.errorType = errorType;
      this.errorParameters = errorParameters;
   }

   public ErrorType getErrorType() {
      return this.errorType;
   }

   public Object[] getErrorParameters() {
      if (this.errorParameters == null) {
         return null;
      } else {
         Object[] ret = new Object[this.errorParameters.length];
         System.arraycopy(this.errorParameters, 0, ret, 0, ret.length);
         return ret;
      }
   }

   public String toString() {
      return this.errorType.toString();
   }
}
