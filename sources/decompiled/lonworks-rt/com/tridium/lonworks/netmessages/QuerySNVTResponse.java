package com.tridium.lonworks.netmessages;

import javax.baja.lonworks.InvalidResponseException;
import javax.baja.lonworks.LonException;
import javax.baja.lonworks.LonMessage;
import javax.baja.lonworks.io.LonInputStream;
import javax.baja.lonworks.io.LonOutputStream;

public class QuerySNVTResponse extends LonMessage implements NetMessages {
   public static final int QUERY_SNVT_RESP_NETLEN = 16;
   private byte[] data = null;

   public QuerySNVTResponse(byte[] data) {
      this.code = 50;
      this.data = data;
   }

   public QuerySNVTResponse(LonInputStream in) throws LonException {
      this.code = 50;
      this.fromInputStream(in);
   }

   @Override
   public void fromInputStream(LonInputStream in) throws LonException {
      int code = in.readUnsigned8();
      if (code != 50) {
         throw new InvalidResponseException(code);
      } else {
         this.data = in.readByteArray();
      }
   }

   public byte[] getData() {
      return this.data;
   }

   public void setData(byte[] data) throws LonException {
      if (data.length > 16) {
         throw new IllegalArgumentException("Invalid data length");
      } else {
         System.arraycopy(data, 0, this.data, 0, data.length);
      }
   }

   @Override
   public void toOutputStream(LonOutputStream out) {
      out.writeUnsigned8(this.code);
      if (this.data != null) {
         out.writeByteArray(this.data);
      }
   }
}
