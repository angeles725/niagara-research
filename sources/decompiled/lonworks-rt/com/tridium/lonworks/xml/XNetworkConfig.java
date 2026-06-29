package com.tridium.lonworks.xml;

import com.tridium.lonworks.util.LonByteArrayUtil;
import java.util.Vector;

public class XNetworkConfig extends XNetVariable implements XIConfig {
   public String scptType = "";
   public String scope = "node";
   public String select = "";
   public String modifyFlag = "anytime";
   public String max = "";
   public String min = "";
   public boolean changeType = false;
   public Vector<XElementQualifier> elems = new Vector<>();

   @Override
   public Vector<XElementQualifier> getElementQualifierVector() {
      return this.elems;
   }

   @Override
   public String getModifyFlag() {
      return this.modifyFlag;
   }

   @Override
   public void setModifyFlag(String modifyFlag) {
      this.modifyFlag = modifyFlag;
   }

   @Override
   public String getMax() {
      return this.max;
   }

   @Override
   public String getMin() {
      return this.min;
   }

   @Override
   public String getInit() {
      return LonByteArrayUtil.toString(this.init);
   }

   @Override
   public void setInit(String s) {
      this.init = LonByteArrayUtil.getBytes(s);
   }
}
