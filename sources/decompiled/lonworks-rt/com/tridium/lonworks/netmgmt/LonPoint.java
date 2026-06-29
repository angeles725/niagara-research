package com.tridium.lonworks.netmgmt;

import com.tridium.lonworks.datatypes.BLinkDescriptor;
import com.tridium.lonworks.enums.BLonLinkStatus;
import com.tridium.lonworks.util.NmUtil;
import javax.baja.lonworks.BINvContainer;
import javax.baja.lonworks.BLocalLonDevice;
import javax.baja.lonworks.BLonDevice;
import javax.baja.lonworks.BLonNetwork;
import javax.baja.lonworks.BNetworkVariable;
import javax.baja.lonworks.datatypes.BAddressEntry;
import javax.baja.lonworks.datatypes.BIAddressEntry;
import javax.baja.lonworks.datatypes.BNvConfigData;
import javax.baja.lonworks.datatypes.BNvProps;
import javax.baja.lonworks.datatypes.BSubnetNode;
import javax.baja.lonworks.enums.BLonLinkType;
import javax.baja.lonworks.enums.BLonNvDirection;
import javax.baja.lonworks.enums.BLonServiceType;
import javax.baja.sys.BComponent;

public class LonPoint implements NetMgmtConst {
   protected BINvContainer nvCntr;
   protected BNetworkVariable origNv;
   protected BLonLinkStatus status;
   protected int addressIndex;
   public int overlappCnt;

   public LonPoint(BINvContainer nvCntr, BNetworkVariable nv) {
      this(nvCntr, nv, BLonLinkStatus.newLink);
   }

   public LonPoint(BINvContainer nvCntr, BNetworkVariable nv, BLonLinkStatus status) {
      this.nvCntr = nvCntr;
      this.status = status;
      this.origNv = nv;
      this.addressIndex = -1;
   }

   protected LonPoint() {
   }

   public Object cloneMe() {
      LonPoint lp = new LonPoint();
      lp.nvCntr = this.nvCntr;
      lp.status = this.status;
      lp.origNv = this.origNv;
      lp.addressIndex = this.addressIndex;
      return lp;
   }

   public BINvContainer getNvContainer() {
      return this.nvCntr;
   }

   public BLonDevice getLonDevice() {
      return this.nvCntr.getLonDevice();
   }

   public int getNvIndex() {
      return this.getNvProps().getNvIndex();
   }

   public int getAddressIndex() {
      return this.addressIndex;
   }

   public BLonLinkStatus getStatus() {
      return this.status;
   }

   public BLonNvDirection getDirection() {
      return this.getNvConfigData().getDirection();
   }

   public BNvProps getNvProps() {
      return this.origNv.getNvProps();
   }

   public BNvConfigData getNvConfigData() {
      return this.origNv.getNvConfigData();
   }

   public BNetworkVariable getNetworkVariable() {
      return this.origNv;
   }

   public boolean isAuthenticated() {
      return this.getNvConfigData().getAuthenticated();
   }

   public boolean isPolled() {
      return this.getNvProps().getPolled();
   }

   public int getOrigSelector() {
      return this.getNvConfigData().getSelector();
   }

   public BNvConfigData getWorkingNvConfigData() {
      return this.getNvConfigData();
   }

   public boolean isSameDevicePoint(LonPoint lp) {
      return this.getLonDevice() == lp.getLonDevice();
   }

   public void setAddressIndex(int addressIndex) {
      this.addressIndex = addressIndex;
   }

   public void setStatus(BLonLinkStatus status) {
      this.status = status;
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

   public boolean isOutput() {
      return this.getDirection() == BLonNvDirection.output;
   }

   public boolean isLocal() {
      return false;
   }

   public boolean isProxy() {
      return false;
   }

   public boolean isPseudo() {
      return false;
   }

   public boolean isError() {
      return this.status.isError();
   }

   public int getSnvtType() {
      return this.getNvProps().getSnvtType();
   }

   public boolean isAliasPoint() {
      return false;
   }

   public String getDeviceName() {
      if (this.nvCntr == null) {
         return "null";
      } else if (this.nvCntr.isLonObject()) {
         return this.nvCntr.getLonDevice().getDisplayName(null) + "." + this.nvCntr.getDisplayName(null);
      } else {
         return ((BComponent)this.nvCntr).getParent() instanceof BLonNetwork
            ? this.nvCntr.getDisplayName(null)
            : ((BComponent)this.nvCntr).getParent().getDisplayName(null) + "." + this.nvCntr.getDisplayName(null);
      }
   }

   public boolean isSameNode(BINvContainer comp) {
      return this.nvCntr.getLonDevice() == comp.getLonDevice();
   }

   public boolean requiresUnbind(boolean isPollOnly) {
      return this.isObsolete() || this.isBound() && isPollOnly;
   }

   public boolean requiresAddressEntry() {
      return this.isPolled() ? this.getDirection() == BLonNvDirection.input : this.getDirection() == BLonNvDirection.output;
   }

   public boolean isPriority() {
      return this.getNvConfigData().getPriority();
   }

   public void clearLocalIndex() {
   }

   public int getAddressGroup() {
      if (this.addressIndex != -1) {
         BIAddressEntry addressEntry = this.getAddressEntry(this.addressIndex);
         if (addressEntry.isGroupAddress()) {
            return addressEntry.getGroupOrSubnet();
         }
      }

      return -1;
   }

   public BIAddressEntry getAddressEntry() {
      return this.addressIndex != -1 ? this.getAddressEntry(this.addressIndex) : null;
   }

   protected BIAddressEntry getAddressEntry(int ndx) {
      return this.nvCntr.getDeviceData().getAddressEntry(ndx);
   }

   public void setAddressEntry(BIAddressEntry entry) {
      this.nvCntr.getDeviceData().setAddressEntry(this.addressIndex, entry);
   }

   public String getNvName() {
      String s = this.getNvProps().getParent().getDisplayName(null);
      return s == null ? "" : s;
   }

   public String getSummaryString() {
      return this.getDeviceName() + ":" + this.getNvName() + "." + this.getNvIndex();
   }

   @Override
   public int hashCode() {
      BSubnetNode sn = this.nvCntr.getDeviceData().getSubnetNodeId();
      return (sn.getSubnetId() << 20) + (sn.getNodeId() << 13) + this.getNvIndex();
   }

   @Override
   public boolean equals(Object point) {
      return point instanceof LonPoint && this.hashCode() == point.hashCode();
   }

   public void changeLinkType(BLonLinkType linkType, boolean priority) {
      if (this.status == BLonLinkStatus.serviceTypeError) {
         this.status = BLonLinkStatus.newLink;
      }

      this.validatePoint(linkType, priority, false);
   }

   public boolean addressIndexIsLocal() {
      BIAddressEntry ae = this.getAddressEntry();
      if (ae != null && this.nvCntr != null) {
         BLocalLonDevice loc = this.nvCntr.getLonNetwork().getLocalLonDevice();
         switch (ae.getAddressType().getOrdinal()) {
            case 1:
               int grp = ae.getGroupOrSubnet();
               BAddressEntry[] aes = loc.getDeviceData().getAddressTable().getAddresses();

               for (int i = 0; i < aes.length; i++) {
                  if (aes[i].getGroupOrSubnet() == grp) {
                     return true;
                  }
               }
               break;
            case 2:
               BSubnetNode sn = loc.getSubnetNodeAddress();
               if (ae.getGroupOrSubnet() == sn.getSubnetId() && ae.getMemberOrNode() == sn.getNodeId()) {
                  return true;
               }
         }

         return false;
      } else {
         return false;
      }
   }

   public boolean validatePoint(BLonLinkType linkType, boolean priority, boolean debug) {
      return this.validatePoint(linkType, priority, debug, false);
   }

   public boolean validatePoint(BLonLinkType linkType, boolean priority, boolean debug, boolean isProxyTgt) {
      if (!this.isActive()) {
         return true;
      } else {
         BNvConfigData cnf = this.getWorkingNvConfigData();
         BLonServiceType svcType = NmUtil.linkTypeToServiceType(linkType);
         if (!this.getNvProps().getServiceConf() && svcType != cnf.getServiceType()) {
            if (debug) {
               System.out.println("service type is not configurable");
            }

            this.status = BLonLinkStatus.serviceTypeError;
            return false;
         } else {
            boolean auth = linkType == BLonLinkType.authenticated;
            if (!this.getNvProps().getAuthConf() && cnf.getAuthenticated() != auth) {
               if (debug) {
                  System.out.println("authenticate is not configurable");
               }

               this.status = BLonLinkStatus.authicateError;
               return false;
            } else if (this.requiresAddressEntry() && !this.getNvProps().getPriorityConf() && cnf.getPriority() != priority) {
               if (debug) {
                  System.out.println("priority is not configurable");
               }

               this.status = BLonLinkStatus.priorityError;
               return false;
            } else {
               if (!this.isLocal() && !this.isPseudo()) {
                  BLonDevice dev = this.nvCntr.getLonDevice();
                  if (!dev.isConfigOnline() || dev.getStatus().isDown()) {
                     if (debug) {
                        System.out.println("not communicative state");
                     }

                     this.status = BLonLinkStatus.deviceError;
                     return false;
                  }
               }

               if (linkType == BLonLinkType.pollOnly) {
                  return true;
               } else {
                  if (this.addressIndex != -1) {
                     BIAddressEntry entry = this.getAddressEntry();
                     if (entry != null && !this.entryMatch(entry, linkType) && this.status == BLonLinkStatus.bound) {
                        if (debug) {
                           System.out.println("descriptors must match");
                        }

                        this.status = BLonLinkStatus.descriptorError;
                        return false;
                     }
                  }

                  if (isProxyTgt && cnf.getServiceType() != svcType && this.status == BLonLinkStatus.bound) {
                     if (debug) {
                        System.out.println("Must match nvConfig service type");
                     }

                     this.status = BLonLinkStatus.newLink;
                     return false;
                  } else if (cnf.getAuthenticated() != auth) {
                     if (debug) {
                        System.out.println("Must match nvConfig authenticate ");
                     }

                     this.status = BLonLinkStatus.newLink;
                     return false;
                  } else if ((this.requiresAddressEntry() || this.isPolled() && this.isOutput()) && cnf.getServiceType() != svcType) {
                     if (debug) {
                        System.out.println("Must match nvConfig service type");
                     }

                     this.status = BLonLinkStatus.newLink;
                     return false;
                  } else if ((this.requiresAddressEntry() || this.isPolled() && this.isOutput()) && cnf.getPriority() != priority) {
                     if (debug) {
                        System.out.println("Must match nvConfig service type priority");
                     }

                     this.status = BLonLinkStatus.newLink;
                     return false;
                  } else {
                     return true;
                  }
               }
            }
         }
      }
   }

   private boolean entryMatch(BIAddressEntry entry, BLonLinkType linkType) {
      if (NmUtil.getLinkType(entry.getDescriptor()) != linkType) {
         return false;
      } else {
         BLinkDescriptor ld = this.getLonDevice().lonNetwork().netmgmt().getLinkDescriptors().getDescriptor(entry.getDescriptor());
         return ld.entryMatches(entry);
      }
   }

   @Override
   public String toString() {
      return this.getSummaryString() + (this.isAuthenticated() ? " authenticate" : "") + " " + this.status + " adrNdx = " + this.addressIndex;
   }
}
