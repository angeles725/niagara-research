package com.tridium.lonworks.netmessages;

import javax.baja.lonworks.LonException;
import javax.baja.lonworks.LonMessage;
import javax.baja.lonworks.io.LonInputStream;
import javax.baja.lonworks.io.LonOutputStream;

public class CapacityInfoRequest extends InstallRequest {
   private int offset = 0;
   private int count = 0;

   public CapacityInfoRequest(int offset, int count) {
      this.code = 112;
      this.setAppCommand(7);
      this.setOffset(offset);
      this.setCount(count);
   }

   public void setOffset(int offset) {
      this.offset = offset;
   }

   public void setCount(int count) {
      this.count = count;
   }

   public int getOffset() {
      return this.offset;
   }

   public int getCount() {
      return this.count;
   }

   @Override
   protected void writeMessageData(LonOutputStream out) {
      out.writeUnsigned16(this.offset);
      out.writeUnsigned8(this.count);
   }

   @Override
   public void readMessageData(LonInputStream in) {
      this.offset = in.readUnsigned16();
      this.count = in.readUnsigned8();
   }

   @Override
   public LonMessage toSuccessMessage(LonInputStream in) throws LonException {
      return new LonMessage(in);
   }
}
