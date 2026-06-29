package com.tridium.lonworks.netmessages;

import javax.baja.lonworks.InvalidResponseException;
import javax.baja.lonworks.LonException;
import javax.baja.lonworks.LonMessage;
import javax.baja.lonworks.io.LonInputStream;
import javax.baja.lonworks.io.LonOutputStream;

public class QueryNodeInfoResponse extends LonMessage implements NetMessages {
   byte[] data;

   public QueryNodeInfoResponse(LonInputStream in) throws LonException {
      this.code = 48;
      this.fromInputStream(in);
   }

   public byte[] getData() {
      return this.data;
   }

   @Override
   public void toOutputStream(LonOutputStream out) {
      out.writeUnsigned8(this.code);
      out.writeByteArray(this.data);
   }

   @Override
   public void fromInputStream(LonInputStream in) throws LonException {
      int code = in.readUnsigned8();
      if (code != 48) {
         throw new InvalidResponseException(code);
      } else {
         this.data = in.readByteArray(in.available());
      }
   }
}
