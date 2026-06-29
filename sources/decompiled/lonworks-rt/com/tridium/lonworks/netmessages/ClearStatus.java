package com.tridium.lonworks.netmessages;

import javax.baja.lonworks.FailedResponseException;
import javax.baja.lonworks.InvalidResponseException;
import javax.baja.lonworks.LonException;
import javax.baja.lonworks.LonMessage;
import javax.baja.lonworks.io.LonInputStream;

public class ClearStatus extends LonMessage implements NetMessages {
   public ClearStatus() {
      this.code = 83;
   }

   @Override
   public LonMessage toResponse(LonInputStream in) throws LonException {
      int code = in.readUnsigned8();
      if (code == 19) {
         throw new FailedResponseException();
      } else if (code != 51) {
         throw new InvalidResponseException(code);
      } else {
         return new NoDataResponse(51);
      }
   }
}
