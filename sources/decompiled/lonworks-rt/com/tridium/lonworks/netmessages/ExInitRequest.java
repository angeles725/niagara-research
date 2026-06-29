package com.tridium.lonworks.netmessages;

import javax.baja.lonworks.io.LonOutputStream;

public class ExInitRequest extends ExNetMgmtCommand {
   int startIndex;
   int endIndex;

   public ExInitRequest(int resource, int startIndex, int endIndex) {
      this.code = 112;
      this.setAppCommand(32);
      this.setResource(resource);
      this.startIndex = startIndex;
      this.endIndex = endIndex;
   }

   public int getStartIndex() {
      return this.startIndex;
   }

   public void setStartIndex(int startIndex) {
      this.startIndex = startIndex;
   }

   public int getendIndex() {
      return this.endIndex;
   }

   public void setendIndex(int endIndex) {
      this.endIndex = endIndex;
   }

   @Override
   public void writeMessageData(LonOutputStream out) {
      out.writeUnsigned16(this.startIndex);
      out.writeUnsigned16(this.endIndex);
   }
}
