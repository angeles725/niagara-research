package com.tridium.lonworks.netmgmt;

import com.tridium.lonworks.Lon;
import com.tridium.lonworks.util.NmUtil;
import java.util.Vector;
import javax.baja.lonworks.AddressManager;
import javax.baja.lonworks.BLonDevice;
import javax.baja.lonworks.datatypes.BAddressEntry;
import javax.baja.lonworks.datatypes.BDeviceData;
import javax.baja.lonworks.datatypes.BIAddressEntry;
import javax.baja.lonworks.enums.BAddressType;
import javax.baja.lonworks.enums.BLonLinkType;

public class Group {
   public static final int STATUS_NEW = 0;
   public static final int STATUS_MODIFIED = 1;
   public static final int STATUS_UPDATED = 2;
   public static final int STATUS_ERROR = 3;
   private int groupNum;
   private BLonLinkType linkType = BLonLinkType.unknown;
   private Vector<GroupMember> members = new Vector<>(32);
   private GroupBitSet groupBits;
   private int size = 0;
   private int expectedSize = 0;
   private int status;

   public Group(int groupNum, int maxMembers) {
      this(groupNum, maxMembers, BLonLinkType.unknown, 0);
   }

   public Group(int groupNum, int maxMembers, BLonLinkType linkType) {
      this(groupNum, maxMembers, linkType, 0);
   }

   public Group(int groupNum, int maxMembers, BLonLinkType linkType, int status) {
      this(groupNum, maxMembers, linkType, status, 0);
   }

   public Group(int groupNum, int maxMembers, BLonLinkType linkType, int status, int expectedSize) {
      this.groupNum = groupNum;
      this.linkType = linkType;
      this.status = status;
      this.expectedSize = expectedSize;
      this.groupBits = new GroupBitSet(maxMembers);
   }

   public int getGroupNum() {
      return this.groupNum;
   }

   public BLonLinkType getLinkType() {
      return this.linkType;
   }

   public GroupBitSet getBitSet() {
      return this.groupBits;
   }

   public int getSize() {
      return this.size;
   }

   public int getExpectedSize() {
      return this.expectedSize;
   }

   public int getStatus() {
      return this.status;
   }

   public GroupMember[] getMembers() {
      GroupMember[] a = new GroupMember[this.members.size()];
      this.members.copyInto(a);
      return a;
   }

   public void setLinkType(BLonLinkType linkType) {
      this.linkType = linkType;
   }

   public void setStatus(int status) {
      this.status = status;
   }

   public boolean isUpdated() {
      return this.status == 2;
   }

   public int[] getMemberIndices() {
      int[] a = new int[this.members.size()];

      for (int i = 0; i < this.members.size(); i++) {
         GroupMember mem = this.members.elementAt(i);
         a[i] = mem.getDeviceIndex();
      }

      return a;
   }

   public BLonDevice[] getMemberDevices(BLonDevice[] lonDevices) {
      BLonDevice[] a = new BLonDevice[this.members.size()];

      for (int i = 0; i < this.members.size(); i++) {
         GroupMember mem = this.members.elementAt(i);
         a[i] = lonDevices[mem.getDeviceIndex()];
      }

      return a;
   }

   public void addMember(GroupMember member) {
      if (!this.isMember(member.getDeviceIndex())) {
         this.members.addElement(member);
         this.groupBits.set(member.getDeviceIndex());
         this.size++;
      }
   }

   public boolean isMember(int devNdx) {
      return this.groupBits.get(devNdx);
   }

   public void removeMember(int devNdx) {
      int numMembers = this.members.size();
      if (this.isMember(devNdx)) {
         this.groupBits.clear(devNdx);
         this.size--;

         for (int i = 0; i < numMembers; i++) {
            if (this.members.elementAt(i).getDeviceIndex() == devNdx) {
               this.members.removeElementAt(i);
               if (this.status == 2) {
                  this.status = 1;
               }

               return;
            }
         }
      }
   }

   public void updateGroup(BLonDevice[] lonDevices, Vector<BLonDevice> offDevs, BLonBindJob bindJob) {
      int numMembers = this.members.size();
      boolean err = false;

      for (int i = 0; i < numMembers; i++) {
         GroupMember member = this.members.elementAt(i);
         BLonDevice dev = lonDevices[member.getDeviceIndex()];
         int desc = NmUtil.linkTypeToDescriptor(this.linkType);
         BIAddressEntry e = BAddressEntry.make(
            BAddressType.group,
            numMembers,
            this.groupNum,
            i,
            desc,
            dev.getDeviceData().getWorkingDomain(),
            NmUtil.getLonNetwork(dev).netmgmt().getLinkDescriptors().getDescriptor(desc)
         );
         BDeviceData dd = dev.getDeviceData();
         int ndx = member.getAddressIndex();
         dd.setAddressEntry(ndx, e, AddressManager.noDeviceChange);

         try {
            if (Lon.n()) {
               NmUtil.setOfflineInBind(dev, offDevs);
               NmUtil.updateAddressTable(dev, ndx);
               if (!NmUtil.verifyAddressEntry(dev, ndx)) {
                  bindJob.error("ERROR: unable to verify address entry " + ndx + " in " + dev.getDisplayName(null), null);
               }
            }
         } catch (Throwable var14) {
            bindJob.error("ERROR: in Group.updateGroup() - comm failure to " + dev.getDisplayName(null), var14);
            err = true;
            e = BAddressEntry.make(
               BAddressType.group,
               0,
               this.groupNum,
               i,
               desc,
               dev.getDeviceData().getWorkingDomain(),
               NmUtil.getLonNetwork(dev).netmgmt().getLinkDescriptors().getDescriptor(desc)
            );
            dd.setAddressEntry(ndx, e);
         }
      }

      this.status = err ? 3 : 2;
   }

   public GroupMember findMember(int devNdx) {
      int numMembers = this.members.size();

      for (int i = 0; i < numMembers; i++) {
         GroupMember member = this.members.elementAt(i);
         if (member.getDeviceIndex() == devNdx) {
            return member;
         }
      }

      return null;
   }

   public boolean hasMessageTag() {
      int numMembers = this.members.size();

      for (int i = 0; i < numMembers; i++) {
         if (this.members.elementAt(i).isMessageTag()) {
            return true;
         }
      }

      return false;
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder();
      int numMembers = this.members.size();
      sb.append("\nGroup #").append(this.groupNum).append(" linkType = ").append(this.linkType);
      sb.append(" size = ").append(numMembers).append(" expected = ").append(this.expectedSize);
      sb.append(" status - ");
      switch (this.status) {
         case 0:
            sb.append("NEW");
            break;
         case 1:
            sb.append("MODIFIED");
            break;
         case 2:
            sb.append("UPDATED");
      }

      for (int i = 0; i < numMembers; i++) {
         sb.append("\n   member #").append(i).append(" ").append(this.members.elementAt(i));
      }

      return sb.toString();
   }
}
