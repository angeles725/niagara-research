package com.tridium.lonworks.netmessages;

import javax.baja.lonworks.InvalidResponseException;
import javax.baja.lonworks.LonException;
import javax.baja.lonworks.LonMessage;
import javax.baja.lonworks.io.LonInputStream;
import javax.baja.lonworks.io.LonOutputStream;

public class FetchNvResponse extends LonMessage implements NetMessages {
   private int nvIndex;
   private byte[] nvData;

   public FetchNvResponse(int nvIndex, byte[] nvData) {
      this.code = 51;
      this.nvIndex = nvIndex;
      this.nvData = nvData;
   }

   public FetchNvResponse(LonInputStream in) throws LonException {
      this.code = 51;
      this.fromInputStream(in);
   }

   public int getNvIndex() {
      return this.nvIndex;
   }

   public void setNvIndex(int nvIndex) {
      this.nvIndex = nvIndex;
   }

   public byte[] getNvData() {
      return this.nvData;
   }

   public int getNvDataLength() {
      return this.nvData.length;
   }

   public void setNvData(byte[] nvData) {
      this.nvData = nvData;
   }

   @Override
   public void toOutputStream(LonOutputStream out) {
      out.writeUnsigned8(this.code);
      if (this.nvIndex > 254) {
         out.writeUnsigned8(255);
         out.writeUnsigned16(this.nvIndex);
      } else {
         out.writeUnsigned8(this.nvIndex);
      }

      out.writeByteArray(this.nvData);
   }

   @Override
   public void fromInputStream(LonInputStream in) throws LonException {
      int code = in.readUnsigned8();
      if (code != 51) {
         throw new InvalidResponseException(code);
      } else {
         this.nvIndex = in.readUnsigned8();
         if (this.nvIndex == 255) {
            this.nvIndex = in.readUnsigned16();
         }

         this.nvData = new byte[in.available()];
         this.nvData = in.readByteArray();
      }
   }
}
