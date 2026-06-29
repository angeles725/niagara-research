package com.tridium.lonworks.netmgmt;

import com.tridium.lonworks.enums.BLonLinkStatus;
import javax.baja.lonworks.BINvContainer;
import javax.baja.lonworks.BNetworkVariable;
import javax.baja.lonworks.datatypes.BNvConfigData;
import javax.baja.lonworks.datatypes.BSubnetNode;

public class LonPointAlias extends LonPoint {
   private int aliasIndex;

   public LonPointAlias(BINvContainer nvCntr, BNetworkVariable nv, BLonLinkStatus status, int aliasIndex) {
      super(nvCntr, nv, status);
      this.aliasIndex = aliasIndex;
   }

   public int getAliasIndex() {
      return this.aliasIndex;
   }

   public void setAliasIndex(int a) {
      this.aliasIndex = a;
   }

   public int getPrimaryNvIndex() {
      return this.getNvIndex();
   }

   @Override
   public BNvConfigData getWorkingNvConfigData() {
      return (BNvConfigData)(this.aliasIndex < 0 ? this.getNvConfigData() : this.getLonDevice().getDeviceData().getAliasTable().getAliasEntry(this.aliasIndex));
   }

   @Override
   public boolean isAliasPoint() {
      return true;
   }

   @Override
   public boolean isLocal() {
      return false;
   }

   @Override
   public int hashCode() {
      BSubnetNode sn = this.nvCntr.getDeviceData().getSubnetNodeId();
      return (sn.getSubnetId() << 20) + (sn.getNodeId() << 13) + this.aliasIndex + 1073741824;
   }

   public int primaryHashCode() {
      return super.hashCode();
   }

   @Override
   public String toString() {
      return "LonPointAlias " + super.toString() + " alias = " + this.aliasIndex;
   }

   @Override
   public String getSummaryString() {
      return super.getSummaryString() + " a(" + this.aliasIndex + ")";
   }
}
