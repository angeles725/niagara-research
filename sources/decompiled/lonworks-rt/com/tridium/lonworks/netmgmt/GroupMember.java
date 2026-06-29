package com.tridium.lonworks.netmgmt;

import javax.baja.lonworks.BLonDevice;
import javax.baja.lonworks.BMessageTag;
import javax.baja.sys.Property;

public class GroupMember {
   BLonDevice dev;
   BMessageTag mtag;
   Property prop;
   private int deviceNdx;
   private int addressNdx;
   private boolean msgTag;

   public GroupMember(int deviceNdx, int addressNdx) {
      this(deviceNdx, addressNdx, false);
   }

   public GroupMember(int deviceNdx, int addressNdx, boolean msgTag) {
      this.deviceNdx = deviceNdx;
      this.addressNdx = addressNdx;
      this.msgTag = msgTag;
   }

   public int getDeviceIndex() {
      return this.deviceNdx;
   }

   public int getAddressIndex() {
      return this.addressNdx;
   }

   public boolean isMessageTag() {
      return this.msgTag;
   }

   @Override
   public String toString() {
      return "deviceNdx " + this.deviceNdx + " address " + this.addressNdx + (this.msgTag ? " messageTag" : " ");
   }
}
