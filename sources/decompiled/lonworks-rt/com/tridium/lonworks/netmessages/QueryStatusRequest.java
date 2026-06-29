package com.tridium.lonworks.netmessages;

import javax.baja.lonworks.FailedResponseException;
import javax.baja.lonworks.InvalidResponseException;
import javax.baja.lonworks.LonException;
import javax.baja.lonworks.LonMessage;
import javax.baja.lonworks.io.LonInputStream;

public class QueryStatusRequest extends LonMessage implements NetMessages {
   public QueryStatusRequest() {
      this.code = 81;
   }

   @Override
   public LonMessage toResponse(LonInputStream in) throws LonException {
      int code = in.readUnsigned8();
      if (code == 17) {
         throw new FailedResponseException();
      } else if (code != 49) {
         throw new InvalidResponseException(code);
      } else {
         in.reset();
         return new QueryStatusResponse(in);
      }
   }
}
