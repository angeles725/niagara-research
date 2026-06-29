package com.tridium.lonworks.xml;

public abstract class XNetVariable extends XLonTyped {
   public String snvtType = "xxx";
   public int index = -1;
   public int averateRate;
   public int maximumRate;
   public int arraySize = 1;
   public boolean offline = false;
   public boolean bindable = true;
   public String direction = "input";
   public String serviceType = "unacked";
   public boolean serviceTypeConfigurable = true;
   public boolean authenticated = false;
   public boolean authenticatedConfigurable = true;
   public boolean priority = false;
   public boolean priorityConfigurable = true;
   public boolean polled = false;
   public boolean sync = false;
   public boolean config = false;
   public boolean freezeChannelPriority = false;

   public int getMaxIndex() {
      int size = this.arraySize == 0 ? 1 : this.arraySize;
      return this.index + size - 1;
   }

   @Override
   public String toString() {
      return "XNetVariable[snvtType=" + this.snvtType + ",index=" + this.index + "]\n" + super.toString();
   }
}
