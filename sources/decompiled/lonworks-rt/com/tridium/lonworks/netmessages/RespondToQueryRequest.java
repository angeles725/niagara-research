package com.tridium.lonworks.netmessages;

import javax.baja.lonworks.FailedResponseException;
import javax.baja.lonworks.InvalidResponseException;
import javax.baja.lonworks.LonException;
import javax.baja.lonworks.LonMessage;
import javax.baja.lonworks.io.LonInputStream;
import javax.baja.lonworks.io.LonOutputStream;

public class RespondToQueryRequest extends LonMessage implements NetMessages {
   public static final int MODE_OFF = 0;
   public static final int MODE_ON = 1;
   public int mode;

   public RespondToQueryRequest(int mode) {
      this.code = 98;
      this.mode = mode;
   }

   public int getMode() {
      return this.mode;
   }

   public void setMode(int index) {
      this.mode = index;
   }

   @Override
   public void toOutputStream(LonOutputStream out) {
      out.writeUnsigned8(this.code);
      out.write(this.mode);
   }

   @Override
   public LonMessage toResponse(LonInputStream in) throws LonException {
      int code = in.readUnsigned8();
      if (code == 2) {
         throw new FailedResponseException();
      } else if (code != 34) {
         throw new InvalidResponseException(code);
      } else {
         return new NoDataResponse(34);
      }
   }
}
