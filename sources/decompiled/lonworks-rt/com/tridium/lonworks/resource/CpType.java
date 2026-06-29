package com.tridium.lonworks.resource;

import com.tridium.lonworks.util.LonByteArrayUtil;
import java.io.IOException;

public class CpType extends Type {
   protected static final int IN_BIT = 128;
   protected static final int IV_BIT = 8;
   protected static final int MN_BIT = 2;
   protected static final int MX_BIT = 1;
   public byte[] min = null;
   public byte[] max = null;
   public byte[] inv = null;
   public byte[] init;
   public boolean inherited;

   public CpType(ResFileInputStream in, String prefix, int scope) throws IOException {
      super(in, prefix, scope);
      int byteArrayControl = in.readUnsigned8();
      if ((byteArrayControl & 2) != 0) {
         this.min = in.readByteArray(this.typeSize);
      }

      if ((byteArrayControl & 1) != 0) {
         this.max = in.readByteArray(this.typeSize);
      }

      this.init = in.readByteArray(this.typeSize);
      if ((byteArrayControl & 8) != 0) {
         this.inv = in.readByteArray(this.typeSize);
      }

      this.inherited = (byteArrayControl & 128) != 0;
   }

   @Override
   public void toString(StringBuffer sb) {
      super.toString(sb);
      sb.append("inherited = ").append(this.inherited).append("\n");
      if (this.min != null) {
         sb.append("min = ").append(LonByteArrayUtil.toString(this.min)).append("\n");
      }

      if (this.max != null) {
         sb.append("max = ").append(LonByteArrayUtil.toString(this.max)).append("\n");
      }

      sb.append("init = ").append(LonByteArrayUtil.toString(this.init)).append("\n");
      if (this.inv != null) {
         sb.append("inv = ").append(LonByteArrayUtil.toString(this.inv)).append("\n");
      }
   }
}
