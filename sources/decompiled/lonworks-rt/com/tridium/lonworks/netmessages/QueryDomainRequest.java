package com.tridium.lonworks.netmessages;

import javax.baja.lonworks.FailedResponseException;
import javax.baja.lonworks.InvalidResponseException;
import javax.baja.lonworks.LonException;
import javax.baja.lonworks.LonMessage;
import javax.baja.lonworks.io.LonInputStream;
import javax.baja.lonworks.io.LonOutputStream;

public class QueryDomainRequest extends LonMessage implements NetMessages {
   private int domainIndex;

   public QueryDomainRequest(int domainIndex) {
      this.code = 106;
      this.domainIndex = domainIndex;
   }

   @Override
   public int getDomainIndex() {
      return this.domainIndex;
   }

   @Override
   public void setDomainIndex(int domainIndex) {
      this.domainIndex = domainIndex;
   }

   @Override
   public void toOutputStream(LonOutputStream out) {
      out.writeUnsigned8(this.code);
      out.write(this.domainIndex);
   }

   @Override
   public void fromInputStream(LonInputStream in) throws LonException {
      int code = in.readUnsigned8();
      if (code != 106) {
         throw new InvalidResponseException(code);
      } else {
         this.domainIndex = in.read();
      }
   }

   @Override
   public LonMessage toResponse(LonInputStream in) throws LonException {
      int code = in.readUnsigned8();
      if (code == 10) {
         throw new FailedResponseException();
      } else if (code != 42) {
         throw new InvalidResponseException(code);
      } else {
         in.reset();
         return new QueryDomainResponse(in);
      }
   }
}
