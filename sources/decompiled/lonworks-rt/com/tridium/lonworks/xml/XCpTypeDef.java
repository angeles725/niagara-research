package com.tridium.lonworks.xml;

import com.tridium.lonworks.util.LonByteArrayUtil;

public class XCpTypeDef extends XTypeDef {
   public boolean inherited = false;
   public byte[] min = null;
   public byte[] max = null;
   public byte[] init = null;

   public String getMin() {
      return this.min != null ? LonByteArrayUtil.toString(this.min) : "";
   }

   public void setMin(String s) {
      this.min = LonByteArrayUtil.getBytes(s);
   }

   public String getMax() {
      return this.max != null ? LonByteArrayUtil.toString(this.max) : "";
   }

   public void setMax(String s) {
      this.max = LonByteArrayUtil.getBytes(s);
   }

   public String getInit() {
      return this.init != null ? LonByteArrayUtil.toString(this.init) : "";
   }

   public void setInit(String s) {
      this.init = LonByteArrayUtil.getBytes(s);
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder(super.toString());
      sb.append(";inherited=").append(this.inherited);
      if (this.min != null) {
         for (int i = 0; i < this.min.length; i++) {
            sb.append(";min[").append(i).append("]=").append(this.min[i]);
         }
      }

      if (this.max != null) {
         for (int i = 0; i < this.max.length; i++) {
            sb.append(";max[").append(i).append("]=").append(this.max[i]);
         }
      }

      if (this.init != null) {
         for (int i = 0; i < this.init.length; i++) {
            sb.append(";init[").append(i).append("]=").append(this.init[i]);
         }
      }

      return sb.toString();
   }

   @Override
   public boolean isCpType() {
      return true;
   }
}
