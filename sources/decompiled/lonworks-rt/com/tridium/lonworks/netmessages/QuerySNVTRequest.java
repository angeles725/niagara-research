package com.tridium.lonworks.netmessages;

import javax.baja.lonworks.FailedResponseException;
import javax.baja.lonworks.InvalidResponseException;
import javax.baja.lonworks.LonException;
import javax.baja.lonworks.LonMessage;
import javax.baja.lonworks.io.LonInputStream;
import javax.baja.lonworks.io.LonOutputStream;

public class QuerySNVTRequest extends LonMessage implements NetMessages {
   private int offset;
   private int count;
   public static final int QUERY_SNVT_REQUEST_LEN = 3;

   public QuerySNVTRequest() {
      this.code = 114;
   }

   public QuerySNVTRequest(int offset, int count) {
      this.code = 114;
      this.offset = offset;
      this.count = count;
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
      out.writeUnsigned16(this.offset);
      out.write(this.count);
   }

   @Override
   public void fromInputStream(LonInputStream in) throws LonException {
      int code = in.readUnsigned8();
      if (code != 114) {
         throw new InvalidResponseException(code);
      } else {
         this.offset = in.readUnsigned16();
         this.count = in.read();
      }
   }

   @Override
   public LonMessage toResponse(LonInputStream in) throws LonException {
      int code = in.readUnsigned8();
      if (code == 18) {
         throw new FailedResponseException();
      } else if (code != 50) {
         throw new InvalidResponseException(code);
      } else {
         in.reset();
         return new QuerySNVTResponse(in);
      }
   }
}
