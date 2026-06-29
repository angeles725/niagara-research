package com.tridium.lonworks.xml;

import com.tridium.lonworks.util.LonByteArrayUtil;
import java.util.Vector;

public class XNetworkVariable extends XNetVariable {
   public String objectIndex = "";
   public int memberIndex = -1;
   public int memberArraySize = 1;
   public boolean mfgMember;
   public boolean changeType = false;
   public Vector<XElementQualifier> elems = new Vector<>();

   @Override
   public Vector<XElementQualifier> getElementQualifierVector() {
      return this.elems;
   }

   @Override
   public String toString() {
      return "XNetworkVariable[objectIndex=" + this.objectIndex + "]\n" + super.toString();
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
