package javax.baja.lonworks;

public class FailedResponseException extends LonException {
   public FailedResponseException() {
      super("Received error response.", null);
   }
}
