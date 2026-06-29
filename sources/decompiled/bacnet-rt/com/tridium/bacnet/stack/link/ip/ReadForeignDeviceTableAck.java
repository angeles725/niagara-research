package com.tridium.bacnet.stack.link.ip;

import com.tridium.bacnet.stack.BacnetInputStream;
import java.io.ByteArrayOutputStream;

public class ReadForeignDeviceTableAck extends BvllMessage {
   private byte[] table;

   ReadForeignDeviceTableAck() {
      super(7);
      this.table = new byte[0];
   }

   ReadForeignDeviceTableAck(int len) {
      super(7, len);
      this.table = new byte[0];
   }

   ReadForeignDeviceTableAck(byte[] table) {
      super(7, table.length);
      this.table = table;
   }

   @Override
   public final byte[] encode(ByteArrayOutputStream out) {
      out.write(129);
      out.write(this.function);
      int bvlcLen = this.table.length + 4;
      out.write(bvlcLen >> 8 & 0xFF);
      out.write(bvlcLen & 0xFF);
      out.write(this.table, 0, this.table.length);
      return out.toByteArray();
   }

   @Override
   public final void decode(BacnetInputStream in) {
      this.table = new byte[this.len];
      in.read(this.table);
   }
}
