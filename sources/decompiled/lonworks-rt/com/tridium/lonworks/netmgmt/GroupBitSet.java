package com.tridium.lonworks.netmgmt;

import java.util.BitSet;
import java.util.logging.Logger;
import javax.baja.lonworks.BLonDevice;

public class GroupBitSet {
   private static final Logger logger = Logger.getLogger("lonworks");
   private BitSet bitSet;
   private int groupSize;

   public GroupBitSet(int size) {
      this.bitSet = new BitSet(size);
      this.groupSize = 0;
   }

   public GroupBitSet(GroupBitSet gps) {
      this.bitSet = (BitSet)gps.bitSet.clone();
      this.groupSize = gps.groupSize;
   }

   public GroupBitSet(BLonDevice[] devices, BLonDevice[] group) {
      int grpLen = group.length;
      this.bitSet = new BitSet(devices.length);

      for (int grpNdx = 0; grpNdx < grpLen; grpNdx++) {
         this.set(findDeviceNdx(devices, group[grpNdx]));
      }
   }

   public GroupBitSet(BLonDevice[] devices, Connection connection) {
      LonPoint hub = connection.getHub();
      LonPoint[] targets = connection.getTargets();
      this.bitSet = new BitSet(devices.length);
      if (hub != null && hub.isActive()) {
         this.set(findDeviceNdx(devices, hub.getLonDevice()));
      }

      for (int i = 0; i < targets.length; i++) {
         if (targets[i].isActive()) {
            this.set(findDeviceNdx(devices, targets[i].getLonDevice()));
         }
      }
   }

   public GroupBitSet(BLonDevice[] devices, LonPoint[] pnts) {
      this.bitSet = new BitSet(devices.length);

      for (int i = 0; i < pnts.length; i++) {
         if (pnts[i].isActive()) {
            this.set(findDeviceNdx(devices, pnts[i].getLonDevice()));
         }
      }
   }

   public GroupBitSet(BLonDevice[] devices, TagConnection connection, boolean includeMtags) {
      TagPoint[] targets = connection.getInputs();
      this.bitSet = new BitSet(devices.length);

      for (int i = 0; i < targets.length; i++) {
         if (targets[i].isActive() && (includeMtags || !targets[i].isMtag())) {
            this.set(findDeviceNdx(devices, targets[i].getLonDevice()));
         }
      }

      if (includeMtags) {
         this.set(findDeviceNdx(devices, connection.getOutput().getLonDevice()));
      }
   }

   public static GroupBitSet getMirror(int len, GroupBitSet gbset) {
      GroupBitSet bs = new GroupBitSet(len);
      int cnt = Math.min(len, gbset.size());

      for (int i = 0; i < cnt; i++) {
         if (!gbset.get(i)) {
            bs.set(i);
         }
      }

      return bs;
   }

   public static int findDeviceNdx(BLonDevice[] devices, BLonDevice dev) {
      int devLen = devices.length;

      for (int devNdx = 0; devNdx < devLen; devNdx++) {
         if (devices[devNdx] == dev) {
            return devNdx;
         }
      }

      logger.severe("ERROR: in GroupBitSet.findDeviceNdx() could not find device " + (dev == null ? null : dev.getDisplayName(null)));
      return 0;
   }

   public boolean contains(GroupBitSet compSet, GroupBitSet excludeSet) {
      BitSet b = (BitSet)compSet.bitSet.clone();
      b.and(this.bitSet);
      if (!b.equals(compSet.bitSet)) {
         return false;
      } else {
         if (excludeSet != null) {
            for (int i = 0; i < this.bitSet.size(); i++) {
               if (excludeSet.bitSet.get(i) && this.bitSet.get(i)) {
                  return false;
               }
            }
         }

         return true;
      }
   }

   @Override
   public boolean equals(Object compSet) {
      return compSet instanceof GroupBitSet && this.bitSet.equals(((GroupBitSet)compSet).bitSet);
   }

   @Override
   public int hashCode() {
      return this.bitSet.hashCode();
   }

   public boolean excludes(GroupBitSet excludeSet) {
      if (excludeSet == null) {
         return true;
      } else {
         for (int i = 0; i < this.bitSet.size(); i++) {
            if (excludeSet.bitSet.get(i) && this.bitSet.get(i)) {
               return false;
            }
         }

         return true;
      }
   }

   public void set(int ndx) {
      if (!this.bitSet.get(ndx)) {
         this.groupSize++;
         this.bitSet.set(ndx);
      }
   }

   public void clear(int ndx) {
      if (this.bitSet.get(ndx)) {
         this.groupSize--;
         this.bitSet.clear(ndx);
      }
   }

   public boolean get(int ndx) {
      return this.bitSet.get(ndx);
   }

   public void or(GroupBitSet gbset) {
      this.bitSet.or(gbset.bitSet);
   }

   public void and(GroupBitSet gbset) {
      this.bitSet.and(gbset.bitSet);
   }

   public void xor(GroupBitSet gbset) {
      this.bitSet.xor(gbset.bitSet);
   }

   @Override
   public String toString() {
      return this.bitSet.toString();
   }

   public int size() {
      return this.bitSet.size();
   }

   public int getGroupSize() {
      return this.groupSize;
   }
}
