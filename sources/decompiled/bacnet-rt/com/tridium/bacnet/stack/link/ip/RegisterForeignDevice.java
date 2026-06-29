package com.tridium.bacnet.stack.link.ip;

import com.tridium.bacnet.stack.BacnetInputStream;
import java.io.ByteArrayOutputStream;

public class RegisterForeignDevice extends BvllMessage {
   public static final int REGISTER_FOREIGN_DEVICE_LENGTH = 6;
   private int timeToLive;

   RegisterForeignDevice() {
      super(5, 6);
   }

   RegisterForeignDevice(int timeToLive) {
      super(5, 6);
      this.timeToLive = timeToLive;
   }

   @Override
   public final byte[] encode(ByteArrayOutputStream out) {
      out.write(129);
      out.write(this.function);
      out.write(0);
      out.write(6);
      out.write(this.timeToLive >> 8 & 0xFF);
      out.write(this.timeToLive & 0xFF);
      return out.toByteArray();
   }

   @Override
   public final void decode(BacnetInputStream in) {
      int msb = in.read();
      int lsb = in.read();
      this.timeToLive = msb << 8 | lsb;
   }
}
