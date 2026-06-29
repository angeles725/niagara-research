package com.tridium.lonworks.netmessages;

import javax.baja.lonworks.FailedResponseException;
import javax.baja.lonworks.InvalidResponseException;
import javax.baja.lonworks.LonException;
import javax.baja.lonworks.LonMessage;
import javax.baja.lonworks.io.LonInputStream;

public class RouterStatusRequest extends LonMessage implements NetMessages {
   public RouterStatusRequest() {
      this.code = 124;
   }

   @Override
   public LonMessage toResponse(LonInputStream in) throws LonException {
      int code = in.readUnsigned8();
      if (code == 28) {
         throw new FailedResponseException();
      } else if (code != 60) {
         throw new InvalidResponseException(code);
      } else {
         in.reset();
         return new RouterStatusResponse(in);
      }
   }
}
