package com.tridium.lonworks.netmessages;

import javax.baja.lonworks.FailedResponseException;
import javax.baja.lonworks.InvalidResponseException;
import javax.baja.lonworks.LonException;
import javax.baja.lonworks.LonMessage;
import javax.baja.lonworks.io.LonInputStream;
import javax.baja.lonworks.io.LonOutputStream;

public class QueryAddrRequest extends LonMessage implements NetMessages {
   private int addrIndex;

   public QueryAddrRequest(int addrIndex) {
      this.code = 103;
      this.addrIndex = addrIndex;
   }

   public int getAddrIndex() {
      return this.addrIndex;
   }

   public void setAddrIndex(int addrIndex) {
      this.addrIndex = addrIndex;
   }

   @Override
   public void toOutputStream(LonOutputStream out) {
      out.writeUnsigned8(this.code);
      out.writeUnsigned8(this.addrIndex);
   }

   @Override
   public void fromInputStream(LonInputStream in) throws LonException {
      int code = in.readUnsigned8();
      if (code != 103) {
         throw new InvalidResponseException(code);
      } else {
         this.addrIndex = in.readUnsigned8();
      }
   }

   @Override
   public LonMessage toResponse(LonInputStream in) throws LonException {
      int code = in.readUnsigned8();
      if (code == 7) {
         throw new FailedResponseException();
      } else if (code != 39) {
         throw new InvalidResponseException(code);
      } else {
         in.reset();
         return new QueryAddrResponse(in);
      }
   }
}
