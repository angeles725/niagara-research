package com.tridium.lonworks.netmgmt;

import com.tridium.lonworks.enums.BLonLinkStatus;
import javax.baja.lonworks.BINvContainer;
import javax.baja.lonworks.BLonDevice;
import javax.baja.lonworks.BNetworkVariable;

public class LonPointRemote extends LonPointLocal {
   private BINvContainer remoteDevice = null;
   private BNetworkVariable remoteNv = null;

   public LonPointRemote(
      BINvContainer nvCntr, BNetworkVariable nv, BLonLinkStatus status, BLonDevice localDevice, BINvContainer remoteDevice, BNetworkVariable remoteNv
   ) {
      super(nvCntr, nv, status, localDevice);
      this.remoteDevice = remoteDevice;
      this.remoteNv = remoteNv;
   }

   private LonPointRemote() {
   }

   @Override
   public Object cloneMe() {
      LonPointRemote lp = new LonPointRemote();
      lp.nvCntr = this.nvCntr;
      lp.origNv = this.origNv;
      lp.status = this.status;
      lp.addressIndex = this.addressIndex;
      lp.overlappCnt = this.overlappCnt;
      lp.localDevice = this.localDevice;
      lp.remoteDevice = this.remoteDevice;
      lp.remoteNv = this.remoteNv;
      return lp;
   }

   @Override
   public String getDeviceName() {
      return this.remoteDevice.getDisplayName(null) + '{' + this.remoteDevice.getLonNetwork().getLogName() + '}';
   }

   @Override
   public String getSummaryString() {
      return "remote." + this.getDeviceName() + ":" + (this.remoteNv != null ? this.remoteNv.getDisplayName(null) : "??");
   }

   @Override
   public String getNvName() {
      return this.remoteNv != null ? this.remoteNv.getDisplayName(null) : "??";
   }

   @Override
   public String toString() {
      return "LonPointRemote " + super.toString();
   }
}
