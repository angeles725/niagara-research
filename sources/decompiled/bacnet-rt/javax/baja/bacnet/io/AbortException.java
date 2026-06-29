package javax.baja.bacnet.io;

import javax.baja.bacnet.BacnetException;
import javax.baja.bacnet.enums.BBacnetAbortReason;
import javax.baja.nre.util.TextUtil;

public class AbortException extends BacnetException {
   private int abortReason;

   public AbortException(int abortReason) {
      super(BBacnetAbortReason.tag(abortReason));
      this.abortReason = abortReason;
   }

   public int getAbortReason() {
      return this.abortReason;
   }

   public String toString() {
      return lex.getText("AbortException.abort") + ":" + TextUtil.toFriendly(this.getMessage());
   }
}
