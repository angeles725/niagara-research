package com.tridium.bacnet.stack.link.ip;

import com.tridium.bacnet.stack.BacnetInputStream;
import java.io.ByteArrayOutputStream;

public class DeleteForeignDeviceTableEntry extends BvllMessage {
   public static final int DELETE_FOREIGN_DEVICE_TABLE_ENTRY_LENGTH = 10;
   private byte[] fdAddress;

   DeleteForeignDeviceTableEntry() {
      super(8, 10);
   }

   DeleteForeignDeviceTableEntry(byte[] fdAddress) {
      super(8, 10);
      this.fdAddress = fdAddress;
   }

   @Override
   public final byte[] encode(ByteArrayOutputStream out) {
      out.write(129);
      out.write(this.function);
      out.write(0);
      out.write(10);
      out.write(this.fdAddress, 0, this.fdAddress.length);
      return out.toByteArray();
   }

   @Override
   public final void decode(BacnetInputStream in) {
      this.fdAddress = new byte[6];
      in.read(this.fdAddress);
   }
}
