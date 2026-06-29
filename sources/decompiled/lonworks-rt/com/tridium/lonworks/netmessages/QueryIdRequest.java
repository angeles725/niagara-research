package com.tridium.lonworks.netmessages;

import javax.baja.lonworks.FailedResponseException;
import javax.baja.lonworks.InvalidResponseException;
import javax.baja.lonworks.LonException;
import javax.baja.lonworks.LonMessage;
import javax.baja.lonworks.io.LonInputStream;
import javax.baja.lonworks.io.LonOutputStream;

public class QueryIdRequest extends LonMessage implements NetMessages {
   private int selector;
   private boolean optional = false;
   private int mode;
   private int offset;
   private int count;
   private byte[] data;
   public static final int UNCONFIGURED = 0;
   public static final int SELECTED = 1;
   public static final int SELECTED_UNCNFG = 2;

   public QueryIdRequest() {
      this.code = 97;
   }

   public QueryIdRequest(int selector) {
      this.code = 97;
      this.selector = selector;
   }

   public QueryIdRequest(int selector, int mode, int offset, byte[] data) {
      this.code = 97;
      this.selector = selector;
      this.optional = true;
      this.mode = mode;
      this.offset = offset;
      this.count = data.length;
      this.data = data;
   }

   public int getSelector() {
      return this.selector;
   }

   public void setSelector(int selector) {
      this.selector = selector;
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

   public byte[] getData() {
      return this.data;
   }

   public void setData(byte[] data) {
      this.data = data;
      this.optional = true;
   }

   @Override
   public void toOutputStream(LonOutputStream out) {
      out.writeUnsigned8(this.code);
      out.write(this.selector);
      if (this.optional) {
         out.write(this.mode);
         out.writeUnsigned16(this.offset);
         out.write(this.data.length);
         out.writeByteArray(this.data);
      }
   }

   @Override
   public void fromInputStream(LonInputStream in) throws LonException {
      int code = in.readUnsigned8();
      if (code != 97) {
         throw new InvalidResponseException(code);
      } else {
         this.selector = in.read();
         this.mode = in.read();
         this.optional = true;
         this.offset = in.readUnsigned16();
         this.count = in.read();
         this.data = new byte[this.count];

         for (int i = 0; i < this.count; i++) {
            this.data[i] = (byte)in.read();
         }
      }
   }

   @Override
   public LonMessage toResponse(LonInputStream in) throws LonException {
      int code = in.readUnsigned8();
      if (code == 1) {
         throw new FailedResponseException();
      } else if (code != 33) {
         throw new InvalidResponseException(code);
      } else {
         in.reset();
         return new QueryIdResponse(in);
      }
   }
}
