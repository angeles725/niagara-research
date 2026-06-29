package com.tridium.lonworks.netmessages;

import javax.baja.lonworks.LonException;
import javax.baja.lonworks.LonMessage;
import javax.baja.lonworks.io.LonInputStream;
import javax.baja.lonworks.io.LonOutputStream;

public class QueryNvInfoRequest extends InstallRequest {
   private int nvInfo = 0;
   private int nvIndex = 0;
   private int sdOffset = 0;
   private int sdLength = 0;

   public QueryNvInfoRequest(int nvIndex, int nvInfo) {
      this.code = 112;
      this.setAppCommand(4);
      this.nvInfo = nvInfo;
      this.nvIndex = nvIndex;
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
      out.writeUnsigned16(this.nvIndex);
      if (this.nvInfo == 3) {
         out.writeUnsigned16(this.sdOffset);
         out.writeUnsigned8(this.sdLength);
      }
   }

   @Override
   public void readMessageData(LonInputStream in) {
      this.nvInfo = in.readUnsigned8();
      this.nvIndex = in.readUnsigned16();
      if (this.nvInfo == 3) {
         this.sdOffset = in.readUnsigned16();
         this.sdLength = in.readUnsigned8();
      }
   }

   @Override
   public LonMessage toSuccessMessage(LonInputStream in) throws LonException {
      return new QueryNvInfoResponse(in);
   }
}
