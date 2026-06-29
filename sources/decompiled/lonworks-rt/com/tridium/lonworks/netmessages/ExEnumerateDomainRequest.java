package com.tridium.lonworks.netmessages;

import javax.baja.lonworks.LonException;
import javax.baja.lonworks.LonMessage;
import javax.baja.lonworks.io.LonInputStream;
import javax.baja.lonworks.io.LonOutputStream;

public class ExEnumerateDomainRequest extends ExNetMgmtCommand {
   private int index = 0;

   public ExEnumerateDomainRequest(int index) {
      this.code = 112;
      this.setAppCommand(38);
      this.setResource(2);
      this.setIndex(index);
   }

   public void setIndex(int index) {
      this.index = index;
   }

   public int getIndex() {
      return this.index;
   }

   @Override
   protected void writeMessageData(LonOutputStream out) {
      out.writeUnsigned16(this.index);
   }

   @Override
   public void readMessageData(LonInputStream in) {
      this.index = in.readUnsigned16();
   }

   @Override
   public LonMessage toSuccessMessage(LonInputStream in) throws LonException {
      return new ExEnumerateDomainResponse(in);
   }
}
