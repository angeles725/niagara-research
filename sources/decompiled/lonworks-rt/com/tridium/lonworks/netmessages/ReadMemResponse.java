package com.tridium.lonworks.netmessages;

import javax.baja.lonworks.InvalidResponseException;
import javax.baja.lonworks.LonException;
import javax.baja.lonworks.LonMessage;
import javax.baja.lonworks.io.LonInputStream;
import javax.baja.lonworks.io.LonOutputStream;

public class ReadMemResponse extends LonMessage implements NetMessages {
   private byte[] data = null;

   public ReadMemResponse(LonInputStream in) throws LonException {
      this.code = in.readUnsigned8();
      if (this.code != 45) {
         throw new InvalidResponseException(this.code);
      } else {
         this.data = in.readByteArray();
      }
   }

   public byte[] getData() {
      return this.data;
   }

   public void setData(byte[] data) throws LonException {
      if (this.data == null || data.length > this.data.length) {
         this.data = new byte[data.length];
      }

      System.arraycopy(data, 0, this.data, 0, data.length);
   }

   @Override
   public void toOutputStream(LonOutputStream out) {
      out.writeUnsigned8(this.code);
      out.writeByteArray(this.data);
   }
}
