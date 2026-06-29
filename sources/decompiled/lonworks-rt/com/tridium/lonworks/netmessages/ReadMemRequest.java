package com.tridium.lonworks.netmessages;

import javax.baja.lonworks.FailedResponseException;
import javax.baja.lonworks.InvalidResponseException;
import javax.baja.lonworks.LonException;
import javax.baja.lonworks.LonMessage;
import javax.baja.lonworks.io.LonInputStream;
import javax.baja.lonworks.io.LonOutputStream;

public class ReadMemRequest extends LonMessage implements NetMessages {
   private int mode;
   private int offset;
   private int count;

   public ReadMemRequest() {
      this.code = 109;
   }

   public ReadMemRequest(int mode, int offset, int count) {
      this.code = 109;
      this.mode = mode;
      this.offset = offset;
      this.count = count;
   }

   public int getMode() {
      return this.mode;
   }

   public void setMode(int mode) {
      this.mode = mode;
   }

   public int getOffset() {
      return this.offset;
   }

   public void setOffset(int offset) {
      this.offset = offset;
   }

   public int getCount() {
      return this.count;
   }

   public void setCount(int count) {
      this.count = count;
   }

   @Override
   public void toOutputStream(LonOutputStream out) {
      out.writeUnsigned8(this.code);
      out.write(this.mode);
      out.writeUnsigned16(this.offset);
      out.write(this.count);
   }

   @Override
   public void fromInputStream(LonInputStream in) throws LonException {
      int code = in.readUnsigned8();
      if (code != 109) {
         throw new InvalidResponseException(code);
      } else {
         this.mode = in.read();
         this.offset = in.readUnsigned16();
         this.count = in.read();
      }
   }

   @Override
   public LonMessage toResponse(LonInputStream in) throws LonException {
      int code = in.readUnsigned8();
      if (code == 13) {
         throw new FailedResponseException();
      } else if (code != 45) {
         throw new InvalidResponseException(code);
      } else {
         in.reset();
         return new ReadMemResponse(in);
      }
   }

   @Override
   public String toString() {
      return "ReadMemRequest:mode = " + this.mode + " offset = " + this.offset + " count = " + this.count;
   }
}
