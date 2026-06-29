package com.tridium.bacnet.stack.link.ip;

import com.tridium.bacnet.stack.BacnetInputStream;
import java.io.ByteArrayOutputStream;

public class ReadBroadcastDistributionTable extends BvllMessage {
   ReadBroadcastDistributionTable() {
      super(2, 4);
   }

   @Override
   public final byte[] encode(ByteArrayOutputStream out) {
      out.write(129);
      out.write(this.function);
      out.write(0);
      out.write(4);
      return out.toByteArray();
   }

   @Override
   public final void decode(BacnetInputStream in) {
   }
}
