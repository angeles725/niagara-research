package com.tridium.lonworks.netmessages;

import javax.baja.lonworks.FailedResponseException;
import javax.baja.lonworks.InvalidResponseException;
import javax.baja.lonworks.LonException;
import javax.baja.lonworks.LonMessage;
import javax.baja.lonworks.io.LonInputStream;
import javax.baja.lonworks.io.LonOutputStream;

public class ChecksumRecalcRequest extends LonMessage implements NetMessages {
   public int checksum;

   public ChecksumRecalcRequest(int checksum) {
      this.code = 111;
      this.checksum = checksum;
   }

   public int getChecksum() {
      return this.checksum;
   }

   public void setChecksum(int checksum) {
      this.checksum = checksum;
   }

   @Override
   public void toOutputStream(LonOutputStream out) {
      out.writeUnsigned8(this.code);
      out.writeUnsigned8(this.checksum);
   }

   @Override
   public void fromInputStream(LonInputStream in) throws LonException {
      int code = in.readUnsigned8();
      if (code != 111) {
         throw new InvalidResponseException(code);
      } else {
         this.setChecksum(in.readUnsigned8());
      }
   }

   @Override
   public LonMessage toResponse(LonInputStream in) throws LonException {
      int code = in.readUnsigned8();
      if (code == 15) {
         throw new FailedResponseException();
      } else if (code != 47) {
         throw new InvalidResponseException(code);
      } else {
         return new NoDataResponse(47);
      }
   }
}
