package com.tridium.lonworks.netmessages;

import com.tridium.lonworks.util.LonByteArrayUtil;
import javax.baja.lonworks.FailedResponseException;
import javax.baja.lonworks.InvalidResponseException;
import javax.baja.lonworks.LonException;
import javax.baja.lonworks.LonMessage;
import javax.baja.lonworks.io.LonInputStream;
import javax.baja.lonworks.io.LonOutputStream;

public class WriteMemRequest extends LonMessage implements NetMessages {
   public static final int MSG_LENGTH_NO_DATA = 6;
   private int mode;
   private int offset;
   private int count;
   private int form;
   private byte[] data = null;

   public WriteMemRequest() {
      this.code = 110;
   }

   public WriteMemRequest(int mode, int offset, int count, int form, byte[] d) {
      this.code = 110;
      this.mode = mode;
      this.offset = offset;
      this.count = count;
      this.form = form;
      this.data = new byte[d.length];
      System.arraycopy(d, 0, this.data, 0, this.data.length);
   }

   public WriteMemRequest(int mode, int offset, int count, int form, byte[] d, int dataLen, int dataOffset) {
      this.code = 110;
      this.mode = mode;
      this.offset = offset;
      this.count = count;
      this.form = form;
      if (dataLen > d.length - dataOffset) {
         dataLen = d.length - dataOffset;
      }

      this.data = new byte[dataLen];
      System.arraycopy(d, dataOffset, this.data, 0, dataLen);
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

   public int getForm() {
      return this.form;
   }

   public void setForm(int form) {
      this.form = form;
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
      out.writeUnsigned8(this.mode);
      out.writeUnsigned16(this.offset);
      out.writeUnsigned8(this.count);
      out.writeUnsigned8(this.form);
      out.writeByteArray(this.data);
   }

   @Override
   public void fromInputStream(LonInputStream in) throws LonException {
      int code = in.readUnsigned8();
      if (code != 110) {
         throw new InvalidResponseException(code);
      } else {
         this.mode = in.readUnsigned8();
         this.offset = in.readUnsigned16();
         this.count = in.readUnsigned8();
         this.form = in.readUnsigned8();
         this.data = in.readByteArray();
      }
   }

   @Override
   public LonMessage toResponse(LonInputStream in) throws LonException {
      int code = in.readUnsigned8();
      if (code == 14) {
         throw new FailedResponseException();
      } else if (code != 46) {
         throw new InvalidResponseException(code);
      } else {
         return new NoDataResponse(46);
      }
   }

   @Override
   public String toString() {
      return "WriteMemRequest:mode = "
         + this.mode
         + " offset = "
         + this.offset
         + " count = "
         + this.count
         + " form = "
         + this.form
         + " data = "
         + LonByteArrayUtil.toString(this.data, ' ');
   }
}
