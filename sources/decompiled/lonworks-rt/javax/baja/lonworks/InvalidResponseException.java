package javax.baja.lonworks;

public class InvalidResponseException extends LonException {
   public InvalidResponseException(int code) {
      super("Invalid message code " + Integer.toString(code, 16), null);
   }
}
