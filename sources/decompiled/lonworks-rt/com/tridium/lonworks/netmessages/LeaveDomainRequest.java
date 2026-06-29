package com.tridium.lonworks.netmessages;

import javax.baja.lonworks.FailedResponseException;
import javax.baja.lonworks.InvalidResponseException;
import javax.baja.lonworks.LonException;
import javax.baja.lonworks.LonMessage;
import javax.baja.lonworks.io.LonInputStream;
import javax.baja.lonworks.io.LonOutputStream;

public class LeaveDomainRequest extends LonMessage implements NetMessages {
   public int domainIndex;

   public LeaveDomainRequest(int domainIndex) {
      this.code = 100;
      this.domainIndex = domainIndex;
   }

   @Override
   public int getDomainIndex() {
      return this.domainIndex;
   }

   @Override
   public void setDomainIndex(int index) {
      this.domainIndex = index;
   }

   @Override
   public void toOutputStream(LonOutputStream out) {
      out.writeUnsigned8(this.code);
      out.write(this.domainIndex);
   }

   @Override
   public LonMessage toResponse(LonInputStream in) throws LonException {
      int code = in.readUnsigned8();
      if (code == 4) {
         throw new FailedResponseException();
      } else if (code != 36) {
         throw new InvalidResponseException(code);
      } else {
         return new NoDataResponse(36);
      }
   }
}
