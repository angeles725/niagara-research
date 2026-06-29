package com.tridium.lonworks.netmgmt;

import com.tridium.lonworks.enums.BLonLinkStatus;
import javax.baja.lonworks.BINvContainer;
import javax.baja.lonworks.BLonDevice;
import javax.baja.lonworks.BNetworkVariable;
import javax.baja.lonworks.enums.BLonNvDirection;

public class LonPointLocal extends LonPoint {
   public static final String LOCAL_DEV = "LocalDev";
   BLonDevice localDevice;

   protected LonPointLocal(BINvContainer nvCntr, BNetworkVariable nv, BLonLinkStatus status, BLonDevice localDevice) {
      super(nvCntr, nv, status);
      this.localDevice = localDevice;
   }

   protected LonPointLocal() {
   }

   @Override
   public boolean isLocal() {
      return true;
   }

   @Override
   public boolean isSameNode(BINvContainer comp) {
      return comp.getLonDevice().isLocal();
   }

   @Override
   public BLonDevice getLonDevice() {
      return this.localDevice;
   }

   @Override
   public boolean isSameDevicePoint(LonPoint lp) {
      return lp.isLocal() && this.getHostDevice() == ((LonPointLocal)lp).getHostDevice();
   }

   private BLonDevice getHostDevice() {
      return super.getLonDevice();
   }

   @Override
   public BLonNvDirection getDirection() {
      return super.getDirection().reverse();
   }

   @Override
   public boolean addressIndexIsLocal() {
      return false;
   }

   @Override
   public int hashCode() {
      return 268435456 + super.hashCode();
   }
}
