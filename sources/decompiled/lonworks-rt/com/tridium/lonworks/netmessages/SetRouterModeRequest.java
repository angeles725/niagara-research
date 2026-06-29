package com.tridium.lonworks.netmessages;

import javax.baja.lonworks.FailedResponseException;
import javax.baja.lonworks.InvalidResponseException;
import javax.baja.lonworks.LonException;
import javax.baja.lonworks.LonMessage;
import javax.baja.lonworks.io.LonInputStream;
import javax.baja.lonworks.io.LonOutputStream;

public class SetRouterModeRequest extends LonMessage implements NetMessages {
   private int mode;

   public SetRouterModeRequest() {
      this.code = 116;
   }

   public SetRouterModeRequest(int mode) {
      this.code = 116;
      if (mode < 0 || mode > 2) {
         mode = 0;
      }

      this.mode = mode;
   }

   public int getMode() {
      return this.mode;
   }

   public void setMode(int mode) {
      this.mode = mode;
   }

   @Override
   public void toOutputStream(LonOutputStream out) {
      out.writeUnsigned8(this.code);
      out.writeUnsigned8(this.mode);
   }

   @Override
   public void fromInputStream(LonInputStream in) throws LonException {
      int code = in.readUnsigned8();
      if (code != 116) {
         throw new InvalidResponseException(code);
      } else {
         this.mode = in.readUnsigned8();
      }
   }

   @Override
   public LonMessage toResponse(LonInputStream in) throws LonException {
      int code = in.readUnsigned8();
      if (code == 20) {
         throw new FailedResponseException();
      } else if (code != 52) {
         throw new InvalidResponseException(code);
      } else {
         return new NoDataResponse(52);
      }
   }
}
