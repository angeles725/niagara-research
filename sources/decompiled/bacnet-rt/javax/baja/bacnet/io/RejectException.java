package javax.baja.bacnet.io;

import javax.baja.bacnet.BacnetException;
import javax.baja.bacnet.enums.BBacnetRejectReason;
import javax.baja.nre.util.TextUtil;

public class RejectException extends BacnetException {
   private int rejectReason;

   public RejectException(int rejectReason) {
      super(BBacnetRejectReason.tag(rejectReason));
      this.rejectReason = rejectReason;
   }

   public int getRejectReason() {
      return this.rejectReason;
   }

   public String toString() {
      return lex.getText("RejectException.reject") + ":" + TextUtil.toFriendly(this.getMessage());
   }
}
