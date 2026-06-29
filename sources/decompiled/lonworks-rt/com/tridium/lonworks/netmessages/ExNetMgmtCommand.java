package com.tridium.lonworks.netmessages;

import javax.baja.lonworks.InvalidResponseException;
import javax.baja.lonworks.LonException;
import javax.baja.lonworks.io.LonInputStream;
import javax.baja.lonworks.io.LonOutputStream;

public abstract class ExNetMgmtCommand extends InstallRequest {
   int resource = 0;

   public int getResource() {
      return this.resource;
   }

   public void setResource(int resource) {
      this.resource = resource;
   }

   @Override
   public void toOutputStream(LonOutputStream out) {
      out.writeUnsigned8(this.code);
      out.writeUnsigned8(this.getAppCommand());
      out.writeUnsigned8(this.getResource());
      this.writeMessageData(out);
   }

   @Override
   public void fromInputStream(LonInputStream in) throws LonException {
      int code = in.readUnsigned8();
      if (code != 112) {
         throw new InvalidResponseException(code);
      } else {
         int appCode = in.readUnsigned8();
         if (appCode != this.getAppCommand()) {
            throw new LonException("Invalid app code " + Integer.toString(appCode, 16));
         } else {
            int resource = in.readUnsigned8();
            if (resource != this.getResource()) {
               throw new LonException("Invalid resource " + Integer.toString(resource, 16));
            } else {
               this.readMessageData(in);
            }
         }
      }
   }
}
