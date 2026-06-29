package com.tridium.lonworks.netmessages;

import javax.baja.lonworks.FailedResponseException;
import javax.baja.lonworks.InvalidResponseException;
import javax.baja.lonworks.LonException;
import javax.baja.lonworks.LonMessage;
import javax.baja.lonworks.io.LonInputStream;

public class QueryAliasRequest extends QueryNvConfigRequest {
   public QueryAliasRequest(int nvIndex) {
      super(nvIndex);
   }

   @Override
   public LonMessage toResponse(LonInputStream in) throws LonException {
      int code = in.readUnsigned8();
      if (code == 8) {
         throw new FailedResponseException();
      } else if (code != 40) {
         throw new InvalidResponseException(code);
      } else {
         in.reset();
         return new QueryAliasResponse(in);
      }
   }
}
