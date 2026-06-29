package com.tridium.lonworks.netmessages;

import com.tridium.lonworks.util.LonByteArrayUtil;
import javax.baja.lonworks.InvalidResponseException;
import javax.baja.lonworks.LonException;
import javax.baja.lonworks.LonMessage;
import javax.baja.lonworks.io.LonInputStream;
import javax.baja.lonworks.io.LonOutputStream;

public class FileXferData extends LonMessage implements NetMessages {
   private int window;
   private int packet;
   private byte[] data;

   public FileXferData() {
      this.code = 62;
   }

   public FileXferData(int window, int packet, byte[] data) {
      this.code = 62;
      this.window = window;
      this.packet = packet;
      this.data = data;
   }

   public int getWindow() {
      return this.window;
   }

   public int getPacket() {
      return this.packet;
   }

   public byte[] getData() {
      return this.data;
   }

   public void setWindow(int window) {
      this.window = window;
   }

   public void setPacket(int packet) {
      this.packet = packet;
   }

   public void setData(byte[] data) {
      this.data = data;
   }

   @Override
   public void toOutputStream(LonOutputStream out) {
      out.writeUnsigned8(this.code);
      byte b = (byte)(this.window << 4 | this.packet & 15);
      out.write(b);
      out.writeByteArray(this.data);
   }

   @Override
   public void fromInputStream(LonInputStream in) throws LonException {
      int code = in.readUnsigned8();
      if (code != 62) {
         throw new InvalidResponseException(code);
      } else {
         try {
            int b = in.read();
            this.window = b >> 4 & 15;
            this.packet = b & 15;
            int dataLen = in.available();
            this.data = new byte[dataLen];
            in.read(this.data, 0, dataLen);
         } catch (Throwable var5) {
         }
      }
   }

   @Override
   public LonMessage toResponse(LonInputStream in) throws LonException {
      int code = in.readUnsigned8();
      if (code > 6) {
         throw new InvalidResponseException(code);
      } else {
         return new FileXferResponse(code);
      }
   }

   @Override
   public String toString() {
      return "window=" + this.window + " packet=" + this.packet + "  data=" + LonByteArrayUtil.toString(this.data);
   }
}
