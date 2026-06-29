package com.tridium.lonworks.netmessages;

import javax.baja.lonworks.FailedResponseException;
import javax.baja.lonworks.InvalidResponseException;
import javax.baja.lonworks.LonException;
import javax.baja.lonworks.LonMessage;
import javax.baja.lonworks.io.LonInputStream;
import javax.baja.lonworks.io.LonOutputStream;

public abstract class InstallRequest extends LonMessage implements NetMessages {
   private int appCommand = 0;

   public InstallRequest() {
      this.code = 112;
   }

   public int getAppCommand() {
      return this.appCommand;
   }

   public void setAppCommand(int appCommand) {
      this.appCommand = appCommand;
   }

   @Override
   public void toOutputStream(LonOutputStream out) {
      out.writeUnsigned8(this.code);
      out.writeUnsigned8(this.getAppCommand());
      this.writeMessageData(out);
   }

   protected void writeMessageData(LonOutputStream out) {
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
            this.readMessageData(in);
         }
      }
   }

   protected void readMessageData(LonInputStream in) {
   }

   @Override
   public LonMessage toResponse(LonInputStream in) throws LonException {
      int code = in.readUnsigned8();
      if (code == 16) {
         throw new FailedResponseException();
      } else if (code != 48) {
         throw new InvalidResponseException(code);
      } else {
         in.reset();
         return this.toSuccessMessage(in);
      }
   }

   public LonMessage toSuccessMessage(LonInputStream in) throws LonException {
      return new NoDataResponse(48);
   }
}
