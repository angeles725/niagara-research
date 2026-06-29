package com.tridium.lonworks.netmessages;

import javax.baja.lonworks.LonException;
import javax.baja.lonworks.LonMessage;
import javax.baja.lonworks.io.LonInputStream;
import javax.baja.lonworks.io.LonOutputStream;

public class QueryNodeInfoRequest extends InstallRequest {
   private int nvInfo = 3;
   private int sdOffset = 0;
   private int sdLength = 0;

   public QueryNodeInfoRequest() {
      this.code = 112;
      this.setAppCommand(5);
   }

   public QueryNodeInfoRequest(int offset, int length) {
      this.code = 112;
      this.setAppCommand(5);
      this.setOffset(offset);
      this.setLength(length);
   }

   public void setOffset(int offset) {
      this.sdOffset = offset;
   }

   public void setLength(int length) {
      this.sdLength = length;
   }

   public int getOffset() {
      return this.sdOffset;
   }

   public int getLength() {
      return this.sdLength;
   }

   @Override
   protected void writeMessageData(LonOutputStream out) {
      out.writeUnsigned8(this.nvInfo);
      if (this.nvInfo == 3) {
         out.writeUnsigned16(this.sdOffset);
         out.writeUnsigned8(this.sdLength);
      }
   }

   @Override
   public void readMessageData(LonInputStream in) {
      this.nvInfo = in.readUnsigned8();
      if (this.nvInfo == 3) {
         this.sdOffset = in.readUnsigned16();
         this.sdLength = in.readUnsigned8();
      }
   }

   @Override
   public LonMessage toSuccessMessage(LonInputStream in) throws LonException {
      return new QueryNodeInfoResponse(in);
   }
}
