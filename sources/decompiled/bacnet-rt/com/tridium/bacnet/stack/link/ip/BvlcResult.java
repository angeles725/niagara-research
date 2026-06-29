package com.tridium.bacnet.stack.link.ip;

import com.tridium.bacnet.stack.BacnetInputStream;
import java.io.ByteArrayOutputStream;

public class BvlcResult extends BvllMessage {
   public static final int BVLC_RESULT_LENGTH = 6;
   private int resultCode = 0;
   public static final BvlcResult OK = new BvlcResult(0);
   public static final BvlcResult WRITE_BDT_NAK = new BvlcResult(16);
   public static final BvlcResult READ_BDT_NAK = new BvlcResult(32);
   public static final BvlcResult REGISTER_FD_NAK = new BvlcResult(48);
   public static final BvlcResult READ_FDT_NAK = new BvlcResult(64);
   public static final BvlcResult DELETE_FDT_ENTRY_NAK = new BvlcResult(80);
   public static final BvlcResult DIST_BCAST_NAK = new BvlcResult(96);

   BvlcResult() {
      super(0, 6);
   }

   BvlcResult(int resultCode) {
      super(0, 6);
      this.resultCode = resultCode;
   }

   @Override
   public final byte[] encode(ByteArrayOutputStream out) {
      out.write(129);
      out.write(this.function);
      out.write(0);
      out.write(6);
      out.write(this.resultCode >> 8 & 0xFF);
      out.write(this.resultCode & 0xFF);
      return out.toByteArray();
   }

   @Override
   public final void decode(BacnetInputStream in) {
      int msb = in.read();
      int lsb = in.read();
      this.resultCode = msb << 8 | lsb;
   }
}
