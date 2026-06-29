package com.tridium.lonworks.netmessages;

import javax.baja.lonworks.InvalidResponseException;
import javax.baja.lonworks.LonException;
import javax.baja.lonworks.LonMessage;
import javax.baja.lonworks.io.LonInputStream;

public class RouterStatusResponse extends LonMessage implements NetMessages {
   public int type;
   public int mode;

   public RouterStatusResponse() {
      this.code = 60;
   }

   public RouterStatusResponse(LonInputStream in) throws LonException {
      this.code = 60;
      this.fromInputStream(in);
   }

   public int getType() {
      return this.type;
   }

   public int getMode() {
      return this.mode;
   }

   @Override
   public void fromInputStream(LonInputStream in) throws LonException {
      int code = in.readUnsigned8();
      if (code != 60) {
         throw new InvalidResponseException(code);
      } else {
         this.type = in.read();
         this.mode = in.read();
      }
   }

   @Override
   public String toString() {
      return "\ntype = " + this.type + "\nmode = " + this.mode;
   }
}
