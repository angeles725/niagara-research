package com.tridium.lonworks.netmgmt;

import com.tridium.lonworks.enums.BLonLinkStatus;
import javax.baja.lonworks.BLonDevice;
import javax.baja.lonworks.datatypes.BDeviceData;
import javax.baja.lonworks.datatypes.BIAddressEntry;
import javax.baja.lonworks.datatypes.BSubnetNode;
import javax.baja.lonworks.enums.BAddressType;
import javax.baja.naming.SlotPath;

public class TagPoint implements NetMgmtConst {
   protected BLonDevice device;
   private int tagIndex;
   private BLonLinkStatus status;
   private String name;
   private boolean mtag = false;

   public TagPoint(BLonDevice device, int tagIndex, BLonLinkStatus status, String name) {
      this.device = device;
      this.tagIndex = tagIndex;
      this.status = status;
      this.name = name;
      this.mtag = tagIndex >= 0;
   }

   public TagPoint(BLonDevice device, int tagIndex, BLonLinkStatus status) {
      this(device, tagIndex, status, "msgOut" + tagIndex);
   }

   public BLonDevice getLonDevice() {
      return this.device;
   }

   public int getTagIndex() {
      return this.tagIndex;
   }

   public BLonLinkStatus getStatus() {
      return this.status;
   }

   public void setTagIndex(int tagIndex) {
      this.tagIndex = tagIndex;
   }

   public void setStatus(BLonLinkStatus status) {
      this.status = status;
   }

   public void setName(String name) {
      this.name = name;
   }

   public boolean isNew() {
      return this.status == BLonLinkStatus.newLink;
   }

   public boolean isObsolete() {
      return this.status == BLonLinkStatus.obsolete;
   }

   public boolean isBound() {
      return this.status == BLonLinkStatus.bound;
   }

   public boolean isUnbound() {
      return this.status == BLonLinkStatus.unbound;
   }

   public boolean isActive() {
      return this.status == BLonLinkStatus.bound || this.status == BLonLinkStatus.newLink;
   }

   public boolean isLocal() {
      return this.device.isLocal();
   }

   public boolean isSameNode(BLonDevice comp) {
      return this.device == comp;
   }

   public boolean isError() {
      return this.status.isError();
   }

   public String getDeviceName() {
      return this.device.getDisplayName(null);
   }

   public String getTagName() {
      return SlotPath.unescape(this.name) + (this.tagIndex >= 0 ? " (" + this.tagIndex + ")" : "");
   }

   public boolean isMtag() {
      return this.mtag;
   }

   public boolean verifyGroup(int group) {
      BDeviceData dd = this.device.getDeviceData();

      for (int i = 0; i < dd.getAddressCount(); i++) {
         BIAddressEntry entry = dd.getAddressEntry(i);
         if (entry.getAddressType() == BAddressType.group && entry.getGroupOrSubnet() == group) {
            return true;
         }
      }

      return false;
   }

   @Override
   public boolean equals(Object point) {
      return point instanceof TagPoint && this.hashCode() == point.hashCode();
   }

   public String getSummaryString() {
      return this.getDeviceName() + ":" + this.name + (this.tagIndex != -1 ? "." + this.tagIndex : "");
   }

   @Override
   public int hashCode() {
      return hashCode(this.device, this.tagIndex);
   }

   public static int hashCode(BLonDevice d, int ndx) {
      BSubnetNode sn = d.getDeviceData().getSubnetNodeId();
      return (sn.getSubnetId() << 11) + (sn.getNodeId() << 4) + ndx;
   }

   @Override
   public String toString() {
      return "Device " + this.getDeviceName() + ":" + this.name + " tagIndex = " + this.tagIndex + " " + this.status;
   }
}
