package com.tridium.lonworks.netmgmt;

import com.tridium.lonworks.Lon;
import com.tridium.lonworks.RouterManager;
import com.tridium.lonworks.datatypes.BLinkDescriptor;
import com.tridium.lonworks.enums.BLonLinkStatus;
import com.tridium.lonworks.util.NmUtil;
import java.util.BitSet;
import java.util.Vector;
import javax.baja.lonworks.BLonDevice;
import javax.baja.lonworks.BLonNetwork;
import javax.baja.lonworks.datatypes.BAddressEntry;
import javax.baja.lonworks.datatypes.BDeviceData;
import javax.baja.lonworks.datatypes.BIAddressEntry;
import javax.baja.lonworks.datatypes.BSubnetNode;
import javax.baja.lonworks.enums.BAddressType;
import javax.baja.lonworks.enums.BLonLinkType;
import javax.baja.sys.BajaRuntimeException;

public class GroupTable implements NetMgmtConst {
   private static int MAX_USE = 5000;
   private static int MAX_SIZE = 256;
   private Group[] table = new Group[256];
   private BLonDevice[] lonDevices;
   private boolean debug;
   private ConnectionTable connTable;
   BLonNetwork lonworks;
   BLonBindJob bindJob;

   public GroupTable(BLonNetwork lonworks, BLonDevice[] lonDevices, boolean debug) {
      this(lonworks, lonDevices, null, debug);
   }

   public GroupTable(BLonNetwork lonworks, BLonDevice[] lonDevices, ConnectionTable connTable, boolean debug) {
      int len = lonDevices.length;
      this.lonDevices = lonDevices;
      this.connTable = connTable;
      this.lonworks = lonworks;
      this.debug = debug;

      for (int ndx = 0; ndx < len; ndx++) {
         BLonDevice dev = lonDevices[ndx];
         if (dev != null) {
            BDeviceData dd = dev.getDeviceData();
            int mtagCount = dd.getMsgTagCount();

            for (int adrNdx = 0; adrNdx < dd.getAddressCount(); adrNdx++) {
               BIAddressEntry entry = dd.getAddressEntry(adrNdx);
               if (entry.isGroupAddress()) {
                  Group grp = this.table[entry.getGroupOrSubnet()];
                  if (grp == null) {
                     grp = new Group(entry.getGroupOrSubnet(), lonDevices.length, NmUtil.getLinkType(entry.getDescriptor()), 2, entry.getSize());
                     this.table[entry.getGroupOrSubnet()] = grp;
                  }

                  if (grp.getExpectedSize() != entry.getSize()) {
                     grp.setStatus(1);
                  }

                  grp.addMember(new GroupMember(ndx, adrNdx, adrNdx < mtagCount));
                  if (entry.getDomain() != dd.getWorkingDomain()) {
                     grp.setStatus(1);
                  }

                  if (grp.getLinkType() == BLonLinkType.unknown && NmUtil.getLinkType(entry.getDescriptor()) != BLonLinkType.unknown) {
                     grp.setLinkType(NmUtil.getLinkType(entry.getDescriptor()));
                  }
               }
            }
         }
      }

      for (int groupNum = 1; groupNum < 256; groupNum++) {
         Group group = this.table[groupNum];
         if (group != null && group.getExpectedSize() != group.getSize() && group.getLinkType() != BLonLinkType.standard) {
            group.setStatus(1);
         }
      }

      if (debug) {
         System.out.print("\nlonDevices = {" + lonDevices[0].getDisplayName(null));

         for (int n = 1; n < lonDevices.length; n++) {
            System.out.print("," + lonDevices[n].getDisplayName(null));
         }

         System.out.println("}");
         System.out.println(this.toString());
      }
   }

   public Group getGroup(int groupNum) {
      return groupNum >= 0 && groupNum <= this.table.length ? this.table[groupNum] : null;
   }

   public boolean selectTurnAround(Connection connection, Vector<BLonDevice> offDevs) {
      LonPoint hub = connection.getHub();
      BLonDevice hubDev = hub.getLonDevice();
      int desc = NmUtil.linkTypeToDescriptor(connection.getLinkType());
      BIAddressEntry addrEntry = BAddressEntry.make(
         BAddressType.turnaround, 0, 0, 1, desc, hubDev.getWorkingDomain(), this.lonworks.netmgmt().getLinkDescriptors().getDescriptor(desc)
      );
      int adrNdx = this.findAddressEntry(hubDev, addrEntry);
      if (adrNdx >= 0) {
         if (hub.getAddressIndex() == adrNdx) {
            if (!addrEntry.equals(hub.getAddressEntry())) {
               hub.setAddressEntry(addrEntry);
               connection.setAddressChange(true);
            } else {
               connection.setAddressChange(false);
            }

            return true;
         }
      } else {
         int ndx = this.connTable.getDeviceIndex(hubDev);
         if (!this.hasFreeAddressEntry(hubDev) && !this.freeAddressEntry(ndx, offDevs) && !this.mergeGroups(ndx, offDevs)) {
            return false;
         }

         adrNdx = this.addAddressEntry(hubDev, addrEntry);
      }

      if (adrNdx < 0) {
         return false;
      } else {
         hub.setAddressIndex(adrNdx);
         hub.setStatus(BLonLinkStatus.newLink);
         connection.setAddressChange(true);
         return true;
      }
   }

   public boolean selectSingle(Connection connection, LonPoint tgt, Vector<BLonDevice> offDevs) {
      LonPoint hub = connection.getHub();
      if (tgt == null) {
         return false;
      } else if (hub.isLocal()) {
         connection.setAddressChange(false);
         return true;
      } else {
         boolean adrHub = hub.requiresAddressEntry();
         LonPoint adrPnt = adrHub ? hub : tgt;
         LonPoint otherPnt = adrHub ? tgt : hub;
         BLonDevice addDev = adrPnt.getLonDevice();
         BLonDevice otherDev = otherPnt.getLonDevice();
         BDeviceData dd = otherDev.getDeviceData();
         int desc = NmUtil.linkTypeToDescriptor(connection.getLinkType());
         BIAddressEntry addrEntry = BAddressEntry.makeSubnetNodeEntry(
            dd.getSubnetNodeId(), desc, addDev.getWorkingDomain(), this.lonworks.netmgmt().getLinkDescriptors().getDescriptor(desc)
         );
         int adrNdx = this.findAddressEntry(addDev, addrEntry);
         if (adrNdx >= 0) {
            if (adrPnt.getAddressIndex() == adrNdx) {
               if (!addrEntry.equals(adrPnt.getAddressEntry())) {
                  adrPnt.setAddressEntry(addrEntry);
                  connection.setAddressChange(true);
               } else {
                  connection.setAddressChange(false);
               }

               return true;
            }
         } else {
            if (!this.hasFreeAddressEntry(addDev) && !this.freeAddressEntry(this.connTable.getDeviceIndex(addDev), true, offDevs)) {
               return false;
            }

            adrNdx = this.addAddressEntry(addDev, addrEntry);
         }

         if (adrNdx < 0) {
            return false;
         } else {
            if (this.debug) {
               System.out.println(" in selectSingle() change adrNdx from " + adrPnt.getAddressIndex() + " to " + adrNdx);
            }

            adrPnt.setAddressIndex(adrNdx);
            if (adrPnt.isBound()) {
               adrPnt.setStatus(BLonLinkStatus.newLink);
            }

            connection.setAddressGroup(-1);
            connection.setAddressChange(true);
            return true;
         }
      }
   }

   public boolean selectGroup(Connection connection, Vector<BLonDevice> offDevs) {
      int groupNum = connection.getAddressGroup();
      GroupBitSet groupBits = connection.getGroupBitSet(this.lonDevices);
      GroupBitSet excludeBits = connection.getExcludeBitSet(this.lonDevices);
      if (groupNum == -1
         || this.table[groupNum] == null
         || !this.table[groupNum].getBitSet().contains(groupBits, excludeBits)
         || this.table[groupNum].getLinkType() != connection.getLinkType()) {
         groupNum = this.selectGroup(groupBits, excludeBits, connection.getLinkType(), false, groupNum, offDevs);
      }

      if (this.debug) {
         System.out.println("\nselected groupNum " + groupNum);
      }

      if (groupNum <= 0) {
         return false;
      } else {
         if (connection.getAddressGroup() == groupNum) {
            connection.setAddressChange(false);
         } else {
            connection.setAddressGroup(groupNum);
            connection.setAddressChange(true);
         }

         this.verifyAddressIndex(connection, this.table[groupNum]);
         return true;
      }
   }

   public boolean reconfigTagGroup(TagConnection tagCnctn, int grpNum, Vector<BLonDevice> offDevs) {
      Group group = this.getGroup(grpNum);
      if (this.debug) {
         System.out.println(" reconfigTagGroup for groupNum = " + grpNum + "\n " + group);
      }

      GroupMember[] grpMems = group.getMembers();
      this.findMemberAndRemove(grpMems, tagCnctn.getOutput().getLonDevice());
      TagPoint[] ins = tagCnctn.getInputs();
      BIAddressEntry addrEntry = tagCnctn.getAddressEntry();

      for (int i = 0; i < ins.length; i++) {
         TagPoint tpIn = ins[i];
         BLonDevice dev = tpIn.getLonDevice();
         GroupMember gMem = this.findMemberAndRemove(grpMems, dev);
         boolean mtag = tpIn.isMtag();
         if (mtag && gMem != null && tpIn.getTagIndex() != gMem.getAddressIndex()) {
            this.removeFromGroup(gMem, group, offDevs);
            gMem = null;
         }

         if (gMem == null) {
            int adrNdx = tpIn.getTagIndex();
            int devNdx = this.connTable.getDeviceIndex(dev);
            if (adrNdx < 0) {
               if (!this.hasFreeAddressEntry(dev) && !this.freeAddressEntry(devNdx, offDevs) && !this.mergeGroups(devNdx, offDevs)) {
                  tagCnctn.setStatus(BLonLinkStatus.dirtyGroup);
                  return false;
               }

               adrNdx = this.addAddressEntry(dev, addrEntry);
               tpIn.setTagIndex(adrNdx);
            }

            group.addMember(new GroupMember(devNdx, adrNdx, mtag));
         }
      }

      for (int i = 0; i < grpMems.length; i++) {
         GroupMember gMemx = grpMems[i];
         if (gMemx != null) {
            this.removeFromGroup(gMemx, group, offDevs);
         }
      }

      group.setStatus(1);
      return true;
   }

   private void removeFromGroup(GroupMember gMem, Group group, Vector<BLonDevice> offDevs) {
      int devNdx = gMem.getDeviceIndex();
      this.clearAddressEntry(this.lonDevices[devNdx], gMem.getAddressIndex(), offDevs);
      group.removeMember(devNdx);
   }

   private GroupMember findMemberAndRemove(GroupMember[] grpMems, BLonDevice dev) {
      int devNdx = this.findDeviceIndex(dev);

      for (int n = 0; n < grpMems.length; n++) {
         GroupMember gMem = grpMems[n];
         if (gMem != null && gMem.getDeviceIndex() == devNdx) {
            grpMems[n] = null;
            return gMem;
         }
      }

      return null;
   }

   public boolean selectTagGroup(TagConnection connection, Vector<BLonDevice> offDevs) {
      GroupBitSet nonMtagBits = new GroupBitSet(this.lonDevices, connection, false);
      int groupNum = this.addGroup(nonMtagBits, BLonLinkType.reliable, 0);
      if (groupNum <= 0) {
         GroupBitSet allMemberBits = new GroupBitSet(this.lonDevices, connection, true);
         GroupBitSet excludeBits = GroupBitSet.getMirror(this.lonDevices.length, allMemberBits);
         groupNum = this.selectGroup(nonMtagBits, excludeBits, BLonLinkType.reliable, true, -1, offDevs);
         if (groupNum > 0) {
            this.fixGroupForTagConn(connection, groupNum, offDevs);
         }
      }

      if (this.debug) {
         System.out.println("\nselected tag groupNum " + groupNum);
      }

      if (groupNum <= 0) {
         return false;
      } else {
         Group grp = this.table[groupNum];
         int memNum = grp.getSize();
         TagPoint output = connection.getOutput();
         BLonDevice odev = output.getLonDevice();
         int outDevIndex = ConnectionTable.getDeviceIndex(this.lonDevices, odev);
         this.table[groupNum].addMember(new GroupMember(outDevIndex, output.getTagIndex(), true));
         TagPoint[] tgts = connection.getInputs();

         for (int i = 0; i < tgts.length; i++) {
            int tNdx = tgts[i].getTagIndex();
            if (tNdx >= 0) {
               int devNdx = ConnectionTable.getDeviceIndex(this.lonDevices, tgts[i].getLonDevice());
               this.table[groupNum].addMember(new GroupMember(devNdx, tNdx, true));
            }
         }

         int size = grp.getSize();
         int desc = NmUtil.linkTypeToDescriptor(grp.getLinkType());
         BIAddressEntry entry = BAddressEntry.make(
            BAddressType.group, size, groupNum, memNum++, desc, odev.getWorkingDomain(), this.lonworks.netmgmt().getLinkDescriptors().getDescriptor(desc)
         );
         this.addAddressEntry(odev, entry, output.getTagIndex());

         for (int ix = 0; ix < tgts.length; ix++) {
            int tNdx = tgts[ix].getTagIndex();
            if (tNdx >= 0) {
               BLonDevice tdev = tgts[ix].getLonDevice();
               entry = BAddressEntry.make(
                  BAddressType.group, size, groupNum, memNum++, desc, tdev.getWorkingDomain(), this.lonworks.netmgmt().getLinkDescriptors().getDescriptor(desc)
               );
               this.addAddressEntry(tdev, entry, tNdx);
            }
         }

         if (connection.getAddressGroup() == groupNum) {
            connection.setAddressChange(false);
         } else {
            connection.setAddressGroup(groupNum);
            connection.setAddressChange(true);
            this.updateAddressIndex(connection, this.table[groupNum]);
         }

         return true;
      }
   }

   private void fixGroupForTagConn(TagConnection tagCnctn, int grpNum, Vector<BLonDevice> offDevs) {
      Group group = this.getGroup(grpNum);
      this.fixMTagPoint(tagCnctn.getOutput(), group, offDevs);
      TagPoint[] ins = tagCnctn.getInputs();

      for (int i = 0; i < ins.length; i++) {
         TagPoint tpIn = ins[i];
         if (tpIn.isMtag()) {
            this.fixMTagPoint(tpIn, group, offDevs);
         }
      }
   }

   private void fixMTagPoint(TagPoint tpIn, Group group, Vector<BLonDevice> offDevs) {
      int devNdx = this.findDeviceIndex(tpIn.getLonDevice());
      GroupMember gMem = group.findMember(devNdx);
      if (gMem != null) {
         if (tpIn.getTagIndex() != gMem.getAddressIndex()) {
            this.clearAddressEntry(tpIn.getLonDevice(), gMem.getAddressIndex(), offDevs);
            group.removeMember(devNdx);
            group.addMember(new GroupMember(devNdx, tpIn.getTagIndex(), true));
         }
      }
   }

   public boolean moveTagPoint(TagPoint tp, int grpNum, Vector<BLonDevice> offDevs) {
      Group group = this.getGroup(grpNum);
      if (group == null) {
         return false;
      } else {
         BLonDevice dev = tp.getLonDevice();
         int devNdx = this.findDeviceIndex(dev);
         if (!this.hasFreeAddressEntry(dev) && !this.freeAddressEntry(devNdx, false, offDevs)) {
            return false;
         } else {
            BIAddressEntry origEntry = dev.getDeviceData().getAddressEntry(tp.getTagIndex());
            this.clearAddressEntry(tp.getLonDevice(), tp.getTagIndex(), offDevs);
            group.removeMember(devNdx);
            BIAddressEntry addrEntry = BAddressEntry.make(origEntry);
            int adrNdx = this.addAddressEntry(dev, addrEntry);
            group.addMember(new GroupMember(devNdx, adrNdx, false));
            return true;
         }
      }
   }

   public void processGroups(Vector<BLonDevice> offDevs) {
      for (int groupNum = 1; groupNum < 256; groupNum++) {
         Group group = this.table[groupNum];
         if (group != null) {
            if (this.connTable.groupUse(groupNum) == 0) {
               this.removeGroup(groupNum, offDevs);
            } else if (group.getStatus() != 2) {
               if (this.debug) {
                  System.out.println("\nUpdate group " + group.getGroupNum() + " " + group);
               }

               group.updateGroup(this.lonDevices, offDevs, this.bindJob);
            }
         }
      }
   }

   public void updateGroupRouteFlags(RouterManager rm) {
      rm.initGroupRouteUpdate();
      int[] chanIds = new int[this.lonDevices.length];

      for (int i = 0; i < chanIds.length; i++) {
         chanIds[i] = this.lonDevices[i] != null ? this.lonDevices[i].getDeviceData().getChannelId() : -1;
      }

      for (int groupNum = 1; groupNum < 256; groupNum++) {
         BitSet channels = new BitSet(rm.getAddressManager().getMaxChannelId() + 1);
         Group group = this.table[groupNum];
         if (group != null) {
            GroupBitSet grp = group.getBitSet();

            for (int i = 0; i < grp.size(); i++) {
               if (grp.get(i)) {
                  channels.set(chanIds[i]);
               }
            }

            rm.setGroupRouteFlags(groupNum, channels);
         }
      }

      rm.updateGroupRouteFlags();
   }

   private int selectGroup(GroupBitSet groupBits, GroupBitSet excludeBits, BLonLinkType eLinkType, boolean mtag, int origGroup, Vector<BLonDevice> offDevs) {
      int bestGrpNum = -1;
      int bestGrpSize = this.lonDevices.length;
      GroupBitSet failedGroup = null;
      if (this.debug) {
         System.out
            .println(
               " in selectGroup :  eLinkType = " + eLinkType + "\n    include: " + groupBits + (excludeBits != null ? "\n    exclude: " + excludeBits : "")
            );
      }

      for (int groupNum = 1; groupNum < 256; groupNum++) {
         Group group = this.table[groupNum];
         if (group != null) {
            if (mtag) {
               if (group.getBitSet().contains(groupBits, excludeBits) && !group.hasMessageTag()) {
                  return groupNum;
               }
            } else if (group.getLinkType() == eLinkType
               && group.getBitSet().contains(groupBits, excludeBits)
               && (bestGrpNum < 0 || group.getSize() < bestGrpSize)) {
               bestGrpNum = groupNum;
               bestGrpSize = group.getSize();
            }
         }
      }

      if (bestGrpNum != -1) {
         return bestGrpNum;
      } else {
         if (origGroup != -1 && this.table[origGroup] != null && this.table[origGroup].getLinkType() == eLinkType && !this.table[origGroup].hasMessageTag()) {
            GroupBitSet grpExcludes = this.connTable.getExcludesForGroup(origGroup);
            GroupBitSet newMembers = this.missingMembers(origGroup, groupBits);
            if (this.debug) {
               System.out.println("attempt expand: new members= " + newMembers + " grpExcludes= " + grpExcludes);
            }

            if (groupBits.excludes(grpExcludes)) {
               boolean failed = false;

               int failedNdx;
               while (!failed && (failedNdx = this.verifyEntries(newMembers)) >= 0) {
                  if (!this.freeAddressEntry(failedNdx, offDevs)) {
                     failed = true;
                  }
               }

               if (!failed) {
                  if (this.debug) {
                     System.out.println("Expand original group " + origGroup);
                  }

                  this.expandGroup(origGroup, newMembers);
                  return origGroup;
               }
            }
         }

         int failedNdxx;
         while ((failedNdxx = this.verifyEntries(groupBits)) >= 0) {
            if (!this.freeAddressEntry(failedNdxx, offDevs)) {
               if (failedGroup == null) {
                  failedGroup = new GroupBitSet(this.lonDevices.length);
               }

               failedGroup.set(failedNdxx);
               groupBits.clear(failedNdxx);
            }
         }

         if (failedGroup == null) {
            return this.addGroup(groupBits, eLinkType, groupBits.getGroupSize());
         } else {
            bestGrpNum = -1;
            bestGrpSize = this.lonDevices.length;

            for (int var16 = 1; var16 < 256; var16++) {
               Group group = this.table[var16];
               if (group != null
                  && !group.hasMessageTag()
                  && group.getLinkType() == eLinkType
                  && group.getBitSet().contains(failedGroup, excludeBits)
                  && groupBits.excludes(this.connTable.getExcludesForGroup(var16))
                  && (bestGrpNum < 0 || group.getSize() < bestGrpSize)) {
                  bestGrpNum = var16;
                  bestGrpSize = group.getSize();
               }
            }

            if (bestGrpNum > 0) {
               if (this.debug) {
                  System.out.println("Expand group " + bestGrpNum + " to include " + failedGroup);
               }

               this.expandGroup(bestGrpNum, this.missingMembers(bestGrpNum, groupBits));
               return bestGrpNum;
            } else {
               boolean failed = false;
               int cnt = this.lonDevices.length;

               for (int ndx = 0; ndx < cnt; ndx++) {
                  if (failedGroup.get(ndx)) {
                     if (!this.mergeGroups(ndx, offDevs)) {
                        failed = true;
                        break;
                     }

                     failedGroup.clear(ndx);
                     groupBits.set(ndx);
                  }
               }

               return !failed ? this.addGroup(groupBits, eLinkType, groupBits.getGroupSize()) : -1;
            }
         }
      }
   }

   private boolean freeAddressEntry(int devNdx, Vector<BLonDevice> offDevs) {
      return this.freeAddressEntry(devNdx, false, offDevs);
   }

   private boolean freeAddressEntry(int devNdx, boolean onlyFreeCheck, Vector<BLonDevice> offDevs) {
      BLonDevice dev = this.lonDevices[devNdx];
      BDeviceData dd = dev.getDeviceData();
      int tabLen = dd.getAddressCount();
      GroupTable.UseCnt[] useCnt = this.connTable.addressEntryUsage(dev);
      if (this.debug && useCnt.length > 0) {
         System.out.print("\nin freeAddressEntry for " + dev.getDisplayName(null) + " - useCnt {" + useCnt[0].cnt);

         for (int ndx = 1; ndx < useCnt.length; ndx++) {
            System.out.print("," + (useCnt[ndx].mtag ? "m" : Integer.toString(useCnt[ndx].cnt)));
         }

         System.out.println("}");

         for (int ndx = 0; ndx < useCnt.length; ndx++) {
            if (useCnt[ndx].include != null) {
               System.out.println("  include " + ndx + " :" + useCnt[ndx].include);
            }

            if (useCnt[ndx].exclude != null) {
               System.out.println("  exclude " + ndx + " :" + useCnt[ndx].exclude);
            }
         }
      }

      int mTagCnt = dd.getMsgTagCount();

      for (int entryNdx = mTagCnt; entryNdx < tabLen; entryNdx++) {
         if (useCnt[entryNdx].cnt == 0) {
            BIAddressEntry entry = dd.getAddressEntry(entryNdx);
            switch (entry.getAddressType().getOrdinal()) {
               case 0:
                  return true;
               case 1:
                  if (this.debug) {
                     System.out.println("\nfound unused group " + entry);
                  }

                  Group grp = this.table[entry.getGroupOrSubnet()];
                  grp.removeMember(devNdx);
                  dd.setAddressEntry(entryNdx, BAddressEntry.DEFAULT);
                  return true;
               case 2:
               case 3:
               case 4:
                  if (this.debug) {
                     System.out.println("\nfound unused address entry " + entry);
                  }

                  dd.setAddressEntry(entryNdx, BAddressEntry.DEFAULT);
                  return true;
            }
         }
      }

      if (onlyFreeCheck) {
         return false;
      } else {
         for (int linkType = 1; linkType <= 2; linkType++) {
            if (this.mergeNodeInGroup(BLonLinkType.make(linkType), dev, useCnt)) {
               if (this.debug) {
                  System.out.println("\nfound s/n which can be merged into group.");
               }

               return true;
            }
         }

         for (int linkTypex = 1; linkTypex <= 2; linkTypex++) {
            if (this.mergeNodesToGroup(BLonLinkType.make(linkTypex), dev, useCnt, offDevs)) {
               return true;
            }
         }

         if (this.mergeGroups(devNdx, offDevs)) {
            return true;
         } else {
            if (this.debug) {
               System.out.println("freeAddress failed");
            }

            return false;
         }
      }
   }

   private boolean mergeGroups(int devNdx, Vector<BLonDevice> offDevs) {
      BLonDevice dev = this.lonDevices[devNdx];
      GroupTable.UseCnt[] useCnt = this.connTable.addressEntryUsage(dev);
      BDeviceData dd = dev.getDeviceData();
      int[] sizOrder = this.getGroupSizeOrder(dd);

      for (int linkType = 1; linkType <= 2; linkType++) {
         int entry1 = -1;
         int entry2 = -1;
         boolean found = false;

         for (int n1 = 0; n1 < sizOrder.length; n1++) {
            int ndx1 = sizOrder[n1];
            BIAddressEntry e1 = dd.getAddressEntry(ndx1);
            if (e1.getDescriptor() == linkType) {
               for (int n2 = n1 + 1; n2 < sizOrder.length; n2++) {
                  int ndx2 = sizOrder[n2];
                  BIAddressEntry e2 = dd.getAddressEntry(ndx2);
                  if (e2.getDescriptor() == linkType) {
                     Group grp1 = this.table[e1.getGroupOrSubnet()];
                     Group grp2 = this.table[e2.getGroupOrSubnet()];
                     if (grp1.getBitSet().excludes(useCnt[ndx2].exclude)
                        && grp2.getBitSet().excludes(useCnt[ndx1].exclude)
                        && !grp1.hasMessageTag()
                        && !grp2.hasMessageTag()) {
                        found = true;
                        entry1 = ndx1;
                        entry2 = ndx2;
                        break;
                     }
                  }
               }

               if (found) {
                  break;
               }
            }
         }

         if (found && entry1 != -1 && entry2 != -1) {
            int groupNum1 = dd.getAddressEntry(entry1).getGroupOrSubnet();
            Group group1 = this.table[groupNum1];
            GroupBitSet grpBits1 = group1.getBitSet();
            int groupNum2 = dd.getAddressEntry(entry2).getGroupOrSubnet();
            Group group2 = this.table[groupNum2];
            GroupBitSet grpBits2 = group2.getBitSet();
            int cnt = this.lonDevices.length;
            if (this.debug) {
               System.out.println("\nattempt to merge groups " + groupNum1 + " & " + groupNum2);
            }

            if (this.debug) {
               System.out.println("in " + dev.getDisplayName(null) + " entry " + entry1 + " " + entry2 + "\n");
            }

            for (int ndx = 0; ndx < cnt; ndx++) {
               if (grpBits1.get(ndx)) {
                  BLonDevice lonDev = this.lonDevices[ndx];
                  int adrNdx2;
                  int adrNdx1;
                  if (grpBits2.get(ndx)) {
                     adrNdx1 = this.findGroupAddressEntry(lonDev, groupNum1);
                     adrNdx2 = this.findGroupAddressEntry(lonDev, groupNum2);
                     lonDev.getDeviceData().setAddressEntry(adrNdx1, BAddressEntry.DEFAULT);
                     if (this.debug) {
                        System.out.println("\n" + lonDev.getDisplayName(null) + " is member of both groups. Remove entry " + adrNdx1);
                     }

                     this.clearAddressEntry(lonDev, adrNdx1, offDevs);
                  } else {
                     adrNdx1 = this.changeGroupAddressEntry(lonDev, groupNum1, groupNum2);
                     adrNdx2 = adrNdx1;
                     group2.addMember(new GroupMember(ndx, adrNdx1));
                     if (this.debug) {
                        System.out
                           .println("\n" + lonDev.getDisplayName(null) + " is only a member of group " + groupNum1 + ". Changed group of entry " + adrNdx1);
                     }
                  }

                  this.connTable.changeEntry(lonDev, adrNdx2, adrNdx1, groupNum2);
               }
            }

            if (group2.getStatus() == 2) {
               group2.setStatus(1);
            }

            this.table[groupNum1] = null;
            return true;
         }
      }

      if (this.debug) {
         System.out.println("unable to find groups to merge in " + dev.getDisplayName(null));
      }

      return false;
   }

   private int findGroupAddressEntry(BLonDevice dev, int groupNum) {
      BDeviceData dd = dev.getDeviceData();
      int adrLen = dd.getAddressCount();

      for (int adrNdx = 0; adrNdx < adrLen; adrNdx++) {
         BIAddressEntry e = dd.getAddressEntry(adrNdx);
         if (e.isGroupAddress() && e.getGroupOrSubnet() == groupNum) {
            return adrNdx;
         }
      }

      return -1;
   }

   private int changeGroupAddressEntry(BLonDevice dev, int origGrp, int newGrp) {
      BDeviceData dd = dev.getDeviceData();
      int adrLen = dd.getAddressCount();

      for (int adrNdx = 0; adrNdx < adrLen; adrNdx++) {
         BIAddressEntry entry = dd.getAddressEntry(adrNdx);
         if (entry.isGroupAddress() && entry.getGroupOrSubnet() == origGrp) {
            BIAddressEntry newEntry = BAddressEntry.make(BAddressType.group, entry.getSize(), newGrp, 0, 0, dd.getWorkingDomain(), new BLinkDescriptor());
            dd.setAddressEntry(adrNdx, newEntry);
            return adrNdx;
         }
      }

      return -1;
   }

   private boolean mergeNodesToGroup(BLonLinkType eLinkType, BLonDevice dev, GroupTable.UseCnt[] useCnt, Vector<BLonDevice> offDevs) {
      BDeviceData dd = dev.getDeviceData();
      int tabLen = dd.getAddressCount();
      int snAdrNdx1 = -1;
      int snDevNdx1 = 0;
      int lowestUse1 = MAX_USE;
      int snAdrNdx2 = -1;
      int snDevNdx2 = 0;
      int lowestUse2 = MAX_USE;

      for (int entryNdx = dd.getMsgTagCount(); entryNdx < tabLen; entryNdx++) {
         BIAddressEntry entry = dd.getAddressEntry(entryNdx);
         if (NmUtil.getLinkType(entry.getDescriptor()) == eLinkType && entry.isSubnetNodeAddress()) {
            BSubnetNode addr = BSubnetNode.make(entry.getGroupOrSubnet(), entry.getMemberOrNode());
            BLonDevice d = this.lonworks.addressManager().getDeviceByAddress(addr);
            if (this.hasFreeAddressEntry(d)) {
               int snNdx = this.findDeviceIndex(d);
               if (lowestUse2 < lowestUse1) {
                  if (useCnt[entryNdx].cnt < lowestUse1) {
                     snDevNdx1 = snNdx;
                     snAdrNdx1 = entryNdx;
                     lowestUse1 = useCnt[entryNdx].cnt;
                  }
               } else if (useCnt[entryNdx].cnt < lowestUse2) {
                  snDevNdx2 = snNdx;
                  lowestUse2 = useCnt[entryNdx].cnt;
                  snAdrNdx2 = entryNdx;
               }
            }
         }
      }

      if (snAdrNdx1 != -1 && snAdrNdx2 != -1) {
         BLonDevice[] grpDev = new BLonDevice[]{dev};
         GroupBitSet groupBits = new GroupBitSet(this.lonDevices, grpDev);
         dd.setAddressEntry(snAdrNdx1, BAddressEntry.DEFAULT);
         groupBits.set(snDevNdx1);
         groupBits.set(snDevNdx2);
         int groupnum = this.addGroup(groupBits, eLinkType, groupBits.getGroupSize());
         if (groupnum <= 0) {
            return false;
         } else {
            this.connTable.changeEntry(dev, snAdrNdx1, snAdrNdx2, groupnum);
            dd.setAddressEntry(snAdrNdx2, BAddressEntry.DEFAULT);
            if (this.debug) {
               System.out.println("\nmerge sn entries " + snAdrNdx1 + " and " + snAdrNdx2 + " into groupnum " + groupnum);
            }

            return true;
         }
      } else {
         return false;
      }
   }

   private boolean mergeNodeInGroup(BLonLinkType linkType, BLonDevice dev, GroupTable.UseCnt[] useCnt) {
      BDeviceData dd = dev.getDeviceData();
      int snAdrNdx = -1;
      int grpAdrNdx = -1;
      int snDevNdx = 0;
      int[] useOrder = this.getUseOrder(useCnt, dd.getMsgTagCount());

      label80:
      for (int sn = 0; sn < useOrder.length; sn++) {
         BIAddressEntry se = dd.getAddressEntry(useOrder[sn]);
         if (NmUtil.getLinkType(se.getDescriptor()) == linkType && se.isSubnetNodeAddress()) {
            BSubnetNode addr = BSubnetNode.make(se.getGroupOrSubnet(), se.getMemberOrNode());
            BLonDevice d = this.lonworks.addressManager().getDeviceByAddress(addr);
            int snNdx = this.findDeviceIndex(d);
            int leastSize = MAX_SIZE;
            grpAdrNdx = -1;
            boolean isMember = false;

            for (int grp = 0; grp < useOrder.length; grp++) {
               int nxt = useOrder[grp];
               BIAddressEntry ge = dd.getAddressEntry(nxt);
               if (!useCnt[nxt].mtag && NmUtil.getLinkType(ge.getDescriptor()) == linkType && ge.isGroupAddress() && !useCnt[nxt].excludes(snNdx)) {
                  Group group = this.table[ge.getGroupOrSubnet()];
                  if (group.isMember(snNdx)) {
                     grpAdrNdx = nxt;
                     isMember = true;
                     if (nxt != -1 && (isMember || this.hasFreeAddressEntry(this.lonDevices[snNdx]))) {
                        snAdrNdx = useOrder[sn];
                        snDevNdx = snNdx;
                        break label80;
                     }
                     continue label80;
                  }

                  if (group.getSize() < leastSize) {
                     leastSize = group.getSize();
                     grpAdrNdx = nxt;
                  }
               }
            }
            break;
         }
      }

      if (snAdrNdx != -1 && grpAdrNdx != -1) {
         BIAddressEntry groupEntry = dd.getAddressEntry(grpAdrNdx);
         int groupNum = groupEntry.getGroupOrSubnet();
         Group grpx = this.table[groupNum];
         if (this.debug) {
            System.out.print("\nmerge sn entry " + snAdrNdx + " with grp entry " + grpAdrNdx);
         }

         if (this.debug) {
            System.out.println("  device " + this.lonDevices[snDevNdx].getDisplayName(null) + " group# " + groupNum);
         }

         if (!grpx.isMember(snDevNdx)) {
            int adrNdx = this.addAddressEntry(this.lonDevices[snDevNdx], groupEntry);
            grpx.addMember(new GroupMember(snDevNdx, adrNdx));
            if (grpx.getStatus() == 2) {
               grpx.setStatus(1);
            }
         }

         this.connTable.changeEntry(dev, grpAdrNdx, snAdrNdx, groupNum);
         dd.setAddressEntry(snAdrNdx, BAddressEntry.DEFAULT);
         return true;
      } else {
         return false;
      }
   }

   private GroupBitSet missingMembers(int groupNum, GroupBitSet mems) {
      GroupBitSet newMembers = new GroupBitSet(mems);
      newMembers.and(this.table[groupNum].getBitSet());
      newMembers.xor(mems);
      return newMembers;
   }

   private void expandGroup(int groupNum, GroupBitSet newMembers) {
      Group group = this.table[groupNum];
      GroupBitSet grpBits = group.getBitSet();
      int member = group.getSize();
      int groupSize = member + newMembers.getGroupSize();

      for (int ndx = 0; ndx < this.lonDevices.length; ndx++) {
         if (newMembers.get(ndx) && !grpBits.get(ndx)) {
            BLonDevice ndev = this.lonDevices[ndx];
            int desc = NmUtil.linkTypeToDescriptor(group.getLinkType());
            int adrNdx = this.addAddressEntry(
               ndev,
               BAddressEntry.make(
                  BAddressType.group,
                  groupSize,
                  groupNum,
                  member++,
                  desc,
                  ndev.getWorkingDomain(),
                  this.lonworks.netmgmt().getLinkDescriptors().getDescriptor(desc)
               )
            );
            group.addMember(new GroupMember(ndx, adrNdx));
         }
      }

      if (group.getStatus() == 2) {
         group.setStatus(1);
      }

      if (this.debug) {
         System.out.println("\nmerged devices " + newMembers + " with existing group. Results =>" + group);
      }
   }

   private int verifyEntries(GroupBitSet groupBits) {
      int cnt = groupBits.size();

      for (int ndx = 0; ndx < cnt; ndx++) {
         if (groupBits.get(ndx) && !this.hasFreeAddressEntry(this.lonDevices[ndx])) {
            return ndx;
         }
      }

      return -1;
   }

   private int addGroup(GroupBitSet groupBits, BLonLinkType eLinkType, int groupSize) {
      int member = 0;
      int cnt = groupBits.size();

      for (int ndx = 0; ndx < cnt; ndx++) {
         if (groupBits.get(ndx) && !this.hasFreeAddressEntry(this.lonDevices[ndx])) {
            if (this.debug) {
               System.out.println("\nNo free addressEntry for " + this.lonDevices[ndx].getDisplayName(null));
            }

            return -1;
         }
      }

      int groupNum = 1;

      while (groupNum < 256 && this.table[groupNum] != null) {
         groupNum++;
      }

      if (groupNum >= 256) {
         return -1;
      } else {
         Group group = new Group(groupNum, this.lonDevices.length, eLinkType, 0);

         for (int ndxx = 0; ndxx < cnt; ndxx++) {
            if (groupBits.get(ndxx)) {
               BLonDevice ndev = this.lonDevices[ndxx];
               int desc = NmUtil.linkTypeToDescriptor(eLinkType);
               BAddressEntry entry = BAddressEntry.make(
                  BAddressType.group,
                  groupSize,
                  groupNum,
                  member++,
                  desc,
                  ndev.getWorkingDomain(),
                  this.lonworks.netmgmt().getLinkDescriptors().getDescriptor(desc)
               );
               int adrNdx = this.addAddressEntry(ndev, entry);
               group.addMember(new GroupMember(ndxx, adrNdx));
            }
         }

         this.table[groupNum] = group;
         if (this.debug) {
            System.out.println("\nadded group" + this.table[groupNum]);
         }

         return groupNum;
      }
   }

   private void removeGroup(int groupNum, Vector<BLonDevice> offDevs) {
      Group group = this.table[groupNum];
      GroupMember[] members = group.getMembers();
      int cnt = members.length;
      if (this.debug) {
         System.out.println("\nRemove group " + groupNum);
      }

      for (int i = 0; i < cnt; i++) {
         GroupMember member = members[i];
         BLonDevice dev = this.lonDevices[member.getDeviceIndex()];
         int addrIndex = member.getAddressIndex();
         this.clearAddressEntry(dev, addrIndex, offDevs);
      }

      this.table[groupNum] = null;
   }

   public void clearAddressEntry(BLonDevice dev, int addrIndex, Vector<BLonDevice> offDevs) {
      dev.getDeviceData().setAddressEntry(addrIndex, BAddressEntry.DEFAULT);

      try {
         if (Lon.n()) {
            NmUtil.setOfflineInBind(dev, offDevs);
            NmUtil.updateAddressTable(dev, addrIndex);
            if (!NmUtil.verifyAddressEntry(dev, addrIndex)) {
               this.bindJob.error("ERROR: unable to verify address entry " + addrIndex + " in " + dev.getDisplayName(null), null);
            }
         }
      } catch (Throwable var5) {
         this.bindJob.error("ERROR: in GroupTable.removeGroup() - comm failure to " + dev.getDisplayName(null), var5);
      }
   }

   private int addAddressEntry(BLonDevice dev, BIAddressEntry newEntry) {
      return this.addAddressEntry(dev, newEntry, -1);
   }

   private int addAddressEntry(BLonDevice dev, BIAddressEntry newEntry, int msgTag) {
      BDeviceData dd = dev.getDeviceData();
      int adrLen = dd.getAddressCount();
      if (msgTag != -1) {
         dd.setAddressEntry(msgTag, newEntry);
         return msgTag;
      } else {
         for (int adrNdx = dd.getMsgTagCount(); adrNdx < adrLen; adrNdx++) {
            if (dd.getAddressEntry(adrNdx).getAddressType() == BAddressType.none) {
               dd.setAddressEntry(adrNdx, newEntry);
               if (this.debug) {
                  System.out.println("\naddAddressEntry in " + dev.getDisplayName(null) + " at index " + adrNdx + "\n" + newEntry);
               }

               return adrNdx;
            }
         }

         return -1;
      }
   }

   private int findAddressEntry(BLonDevice dev, BIAddressEntry entry) {
      BDeviceData dd = dev.getDeviceData();
      int adrLen = dd.getAddressCount();

      for (int adrNdx = 0; adrNdx < adrLen; adrNdx++) {
         if (dd.getAddressEntry(adrNdx).isSameAddress(entry)) {
            return adrNdx;
         }
      }

      return -1;
   }

   private boolean hasFreeAddressEntry(BLonDevice dev) {
      BDeviceData dd = dev.getDeviceData();
      int adrLen = dd.getAddressCount();

      for (int adrNdx = dd.getMsgTagCount(); adrNdx < adrLen; adrNdx++) {
         BIAddressEntry e = dd.getAddressEntry(adrNdx);
         if (e.getAddressType() == BAddressType.none) {
            return true;
         }
      }

      return false;
   }

   public int findDeviceIndex(BLonDevice dev) {
      for (int i = 0; i < this.lonDevices.length; i++) {
         if (this.lonDevices[i] == dev) {
            return i;
         }
      }

      throw new BajaRuntimeException("INTERNAL ERROR: Could not find lonDevice " + dev.getDisplayName(null) + " in list.");
   }

   public BLonDevice[] getDevicesForGroup(int groupNum) {
      Group grp = this.getGroup(groupNum);
      return grp == null ? null : grp.getMemberDevices(this.lonDevices);
   }

   private void verifyAddressIndex(Connection con, Group grp) {
      LonPoint pnt = con.getHub();
      if (pnt.requiresAddressEntry()) {
         this.verifyAddressIndex(pnt, grp);
      }

      LonPoint[] targets = con.getTargets();

      for (int i = 0; i < targets.length; i++) {
         pnt = targets[i];
         if (pnt.requiresAddressEntry()) {
            this.verifyAddressIndex(pnt, grp);
         }
      }
   }

   private void verifyAddressIndex(LonPoint pnt, Group grp) {
      GroupMember member = grp.findMember(this.findDeviceIndex(pnt.getLonDevice()));
      if (member == null) {
         pnt.setStatus(BLonLinkStatus.error);
      } else {
         int index = member.getAddressIndex();
         if (index != pnt.getAddressIndex()) {
            pnt.setAddressIndex(index);
            if (this.debug) {
               System.out.println(" change address index in " + pnt);
            }

            if (pnt.isBound()) {
               pnt.setStatus(BLonLinkStatus.newLink);
            }
         }
      }
   }

   public void updateAddressIndex(TagConnection con, Group grp) {
      TagPoint[] ins = con.getInputs();

      for (int i = 0; i < ins.length; i++) {
         TagPoint pnt = ins[i];
         GroupMember member = grp.findMember(this.findDeviceIndex(pnt.getLonDevice()));
         if (member == null) {
            pnt.setStatus(BLonLinkStatus.error);
         } else {
            pnt.setTagIndex(member.getAddressIndex());
            if (this.debug & !pnt.isNew()) {
               System.out.println(" change address index in " + pnt);
            }

            if (pnt.isBound()) {
               pnt.setStatus(BLonLinkStatus.newLink);
            }
         }
      }
   }

   private int[] getUseOrder(GroupTable.UseCnt[] useCnt, int mtagCnt) {
      int len = useCnt.length - mtagCnt;
      int[] useOrder = new int[len];
      int i = 0;

      while (i < len) {
         useOrder[i] = i++;
      }

      i = 0;

      while (i < len - 1) {
         if (useCnt[useOrder[i]].cnt > useCnt[useOrder[i + 1]].cnt) {
            int val = useOrder[i];
            useOrder[i] = useOrder[i + 1];
            useOrder[i + 1] = val;
            i = i == 0 ? 1 : i - 1;
         } else {
            i++;
         }
      }

      return useOrder;
   }

   private int[] getGroupSizeOrder(BDeviceData dd) {
      int mtagCnt = dd.getMsgTagCount();
      int cnt = 0;

      for (int i = mtagCnt; i < dd.getAddressCount(); i++) {
         if (dd.getAddressEntry(i).isGroupAddress()) {
            cnt++;
         }
      }

      int[] sizOrder = new int[cnt];
      cnt = 0;

      for (int ix = mtagCnt; ix < dd.getAddressCount(); ix++) {
         if (dd.getAddressEntry(ix).isGroupAddress()) {
            sizOrder[cnt++] = ix;
         }
      }

      int n = 0;

      while (n < cnt - 1) {
         if (this.table[dd.getAddressEntry(sizOrder[n]).getGroupOrSubnet()].getSize()
            > this.table[dd.getAddressEntry(sizOrder[n + 1]).getGroupOrSubnet()].getSize()) {
            int val = sizOrder[n];
            sizOrder[n] = sizOrder[n + 1];
            sizOrder[n + 1] = val;
            n = n == 0 ? 1 : n - 1;
         } else {
            n++;
         }
      }

      return sizOrder;
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("\nGroup Table\n===================================\n");

      for (int i = 0; i < this.table.length; i++) {
         if (this.table[i] != null) {
            sb.append(this.table[i]);
         }
      }

      return sb.toString();
   }

   public Group[] getGroupArray() {
      return this.table;
   }

   public static class UseCnt {
      boolean mtag = false;
      int cnt = 0;
      GroupBitSet exclude = null;
      GroupBitSet include = null;

      boolean excludes(int ndx) {
         return this.exclude == null ? false : this.exclude.get(ndx);
      }
   }
}
