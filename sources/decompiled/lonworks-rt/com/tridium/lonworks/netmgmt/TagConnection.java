package com.tridium.lonworks.netmgmt;

import com.tridium.lonworks.datatypes.BLinkDescriptor;
import com.tridium.lonworks.enums.BLonLinkStatus;
import java.util.Vector;
import javax.baja.lonworks.BLonDevice;
import javax.baja.lonworks.datatypes.BIAddressEntry;
import javax.baja.lonworks.datatypes.BSubnetNode;
import javax.baja.lonworks.enums.BAddressType;

public class TagConnection implements NetMgmtConst {
   private int addressGroup = -1;
   private BIAddressEntry entry = null;
   private boolean addressChange = true;
   private Vector<TagPoint> inputs = new Vector<>(10);
   private TagPoint output;

   public TagConnection(TagPoint pnt, boolean debug) {
      this.output = pnt;
   }

   public void setAddressChange(boolean addressChange) {
      this.addressChange = addressChange;
   }

   public void setAddressGroup(int addressGroup) {
      this.addressGroup = addressGroup;
   }

   public void setAddressEntry(BIAddressEntry entry) {
      this.entry = entry;
      if (entry.isGroupAddress()) {
         this.addressGroup = entry.getGroupOrSubnet();
      }
   }

   public int getAddressGroup() {
      return this.addressGroup;
   }

   public boolean isAddressChange() {
      return this.addressChange;
   }

   public BIAddressEntry getAddressEntry() {
      return this.entry;
   }

   public TagPoint getOutput() {
      return this.output;
   }

   public TagPoint[] getInputs() {
      TagPoint[] a = new TagPoint[this.inputs.size()];
      this.inputs.copyInto(a);
      return a;
   }

   public boolean hasInput() {
      return this.inputs.size() > 0;
   }

   public boolean containsDevice(BLonDevice dev) {
      return this.findDevice(dev) != null;
   }

   public TagPoint findDevice(BLonDevice dev) {
      if (this.output.isSameNode(dev)) {
         return this.output;
      } else {
         int cnt = this.inputs.size();

         for (int i = 0; i < cnt; i++) {
            TagPoint tp = this.inputs.elementAt(i);
            if (tp.isSameNode(dev)) {
               return tp;
            }
         }

         return null;
      }
   }

   public int getGroupNum() {
      return this.entry != null && this.entry.isGroupAddress() ? this.entry.getGroupOrSubnet() : -1;
   }

   public boolean isNew() {
      int cnt = this.inputs.size();

      for (int i = 0; i < cnt; i++) {
         if (this.inputs.elementAt(i).isNew()) {
            return true;
         }
      }

      return this.output.isNew();
   }

   public void setError() {
      this.setStatus(BLonLinkStatus.error);
   }

   public void setStatus(BLonLinkStatus status) {
      int cnt = this.inputs.size();

      for (int i = 0; i < cnt; i++) {
         this.inputs.elementAt(i).setStatus(status);
      }

      this.output.setStatus(status);
   }

   public boolean isError() {
      if (this.output.isError()) {
         return true;
      } else {
         int cnt = this.inputs.size();

         for (int i = 0; i < cnt; i++) {
            if (this.inputs.elementAt(i).isError()) {
               return true;
            }
         }

         return false;
      }
   }

   public boolean isBound() {
      if (!this.output.isBound()) {
         return false;
      } else {
         int cnt = this.inputs.size();

         for (int i = 0; i < cnt; i++) {
            if (!this.inputs.elementAt(i).isBound()) {
               return false;
            }
         }

         return true;
      }
   }

   public boolean isActive() {
      if (!this.output.isActive()) {
         return false;
      } else {
         int cnt = this.inputs.size();

         for (int i = 0; i < cnt; i++) {
            if (this.inputs.elementAt(i).isActive()) {
               return true;
            }
         }

         return false;
      }
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder();
      int cnt = this.inputs.size();
      sb.append(" TagConnection => ");
      sb.append("\n   Output = ").append(this.output);
      if (cnt == 0) {
         sb.append("  no inputs");
      }

      for (int i = 0; i < cnt; i++) {
         sb.append("\n   input #").append(i).append(" ").append(this.inputs.elementAt(i));
      }

      return sb.toString();
   }

   public boolean addInput(TagPoint newPoint) {
      int cnt = this.inputs.size();

      for (int i = 0; i < cnt; i++) {
         if (this.inputs.elementAt(i).equals(newPoint)) {
            return true;
         }
      }

      this.inputs.addElement(newPoint);
      return true;
   }

   public void setMessageTagStatus(ConnectionTable ct, GroupTable groupTable) {
      if (this.entry != null) {
         if (!this.output.isObsolete() || this.inputs.size() != 0) {
            BLinkDescriptor desc = ct.lonworks.netmgmt().getLinkDescriptors().getDescriptor(2);
            boolean dirtyD = !desc.entryMatches(this.entry);
            BAddressType type = this.entry.getAddressType();
            int numInputs = this.inputs.size();
            if (type != BAddressType.group) {
               if (type == BAddressType.subnetNode) {
                  BLonLinkStatus status = BLonLinkStatus.newLink;
                  if (numInputs == 1) {
                     TagPoint in = this.inputs.elementAt(0);
                     BSubnetNode adr = in.getLonDevice().getSubnetNodeAddress();
                     if (adr.getSubnetId() == this.entry.getGroupOrSubnet() && adr.getNodeId() == this.entry.getMemberOrNode()) {
                        if (dirtyD) {
                           status = BLonLinkStatus.dirtyDescriptor;
                        } else {
                           status = BLonLinkStatus.bound;
                        }
                     }
                  }

                  this.setStatus(status);
               } else {
                  System.out.println("\nERROR: in TagConnection.verifyEntry()");
                  this.setStatus(BLonLinkStatus.error);
               }
            } else {
               TagPoint in = this.inputs.elementAt(0);
               if (numInputs == 1 && !in.isMtag()) {
                  this.setStatus(BLonLinkStatus.newLink);
                  return;
               }

               int groupNum = this.entry.getGroupOrSubnet();
               Group group = groupTable.getGroup(groupNum);
               boolean stNew = false;
               if (group == null) {
                  this.setStatus(BLonLinkStatus.newLink);
                  return;
               }

               if (group.getSize() != numInputs + 1) {
                  stNew = true;
               }

               for (int i = 0; i < numInputs; i++) {
                  in = this.inputs.elementAt(i);
                  GroupMember gMem = group.findMember(ct.getDeviceIndex(in.getLonDevice()));
                  if (gMem != null && (!in.isMtag() || gMem.getAddressIndex() == in.getTagIndex())) {
                     in.setTagIndex(gMem.getAddressIndex());
                  } else {
                     stNew = true;
                  }
               }

               if (stNew) {
                  this.setStatus(BLonLinkStatus.newLink);
                  return;
               }

               GroupMember gMem = group.findMember(ct.getDeviceIndex(this.output.getLonDevice()));
               if (gMem == null || !gMem.isMessageTag() || gMem.getAddressIndex() != this.output.getTagIndex()) {
                  this.setStatus(BLonLinkStatus.newLink);
                  return;
               }

               if (dirtyD) {
                  this.setStatus(BLonLinkStatus.dirtyDescriptor);
                  return;
               }

               this.setStatus(BLonLinkStatus.bound);
            }
         }
      }
   }

   public void setUpdatedStatus() {
      if (!this.isError()) {
         if (this.output.isNew()) {
            this.setStatus(BLonLinkStatus.bound);
         } else if (this.output.isObsolete()) {
            this.setStatus(BLonLinkStatus.unbound);
         }
      }
   }

   public boolean hasNonMtagPoint() {
      int cnt = this.inputs.size();

      for (int i = 0; i < cnt; i++) {
         if (!this.inputs.elementAt(i).isMtag()) {
            return true;
         }
      }

      return false;
   }
}
