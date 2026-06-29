package com.tridium.lonworks;

import com.tridium.lonworks.datatypes.BLonRouteTable;
import com.tridium.lonworks.enums.BLonRouterMode;
import com.tridium.lonworks.enums.BLonRouterType;
import com.tridium.lonworks.netmessages.NetMessages;
import com.tridium.lonworks.netmgmt.NetMgmtConst;
import com.tridium.lonworks.util.LonByteArrayUtil;
import com.tridium.lonworks.util.RouterUtil;
import java.util.BitSet;
import javax.baja.lonworks.BLonNetwork;
import javax.baja.lonworks.LonException;
import javax.baja.spy.SpyWriter;
import javax.baja.sys.BajaRuntimeException;

public class RouterManager implements NetMgmtConst, NetMessages {
   private NAddressManager addMan;
   private RouterManager.Router[] rtrTab;
   private int maxChanId;
   private BLonNetwork lon;

   public RouterManager(NAddressManager addMan, BLonNetwork lonworks) {
      this.addMan = addMan;
      this.lon = lonworks;
   }

   public void update() {
      BLonRouter[] routers = this.addMan.getRouterList();
      this.rtrTab = new RouterManager.Router[routers.length];

      for (int i = 0; i < routers.length; i++) {
         RouterManager.Router rtr = new RouterManager.Router();
         rtr.nearChan = routers[i].getNearDeviceData().getChannelId();
         rtr.farChan = routers[i].getFarDeviceData().getChannelId();
         rtr.nearGroupTable = this.newRouteTableArray();
         rtr.farGroupTable = this.newRouteTableArray();
         this.rtrTab[i] = rtr;
      }

      this.maxChanId = this.addMan.getMaxChannelId();

      for (int i = 0; i < this.rtrTab.length; i++) {
         BitSet bitSet = new BitSet(this.maxChanId + 1);
         this.setFarChannelBits(bitSet, this.rtrTab[i], this.maxChanId);
         this.rtrTab[i].farChannels = bitSet;
      }
   }

   private void setFarChannelBits(BitSet flags, RouterManager.Router rtr, int maxDepth) {
      if (maxDepth == 0) {
         String err = "INTERNAL ERROR int RouterManager - illegal network connection";
         this.lon.log().severe(err);
         throw new BajaRuntimeException(err);
      } else {
         flags.set(rtr.farChan);

         for (int i = 0; i < this.rtrTab.length; i++) {
            if (this.rtrTab[i].nearChan == rtr.farChan) {
               this.setFarChannelBits(flags, this.rtrTab[i], maxDepth - 1);
            }
         }
      }
   }

   public void updateRouteTables(BLonRouter lonRouter) {
      RouterManager.Router rtr = this.getRouter(lonRouter);
      BLonRouteTable farRoute = this.getFarRouteTable(rtr);
      lonRouter.setFarSubnetTable(farRoute);
      BLonRouteTable nearRoute = farRoute.invert(this.addMan.getSubnetMap());
      lonRouter.setNearSubnetTable(nearRoute);
   }

   private BLonRouteTable getFarRouteTable(RouterManager.Router rtr) {
      BitSet farChannels = rtr.farChannels;
      byte[] a = this.newRouteTableArray();
      this.clearFlag(0, a);

      for (int id = 0; id < farChannels.size(); id++) {
         if (farChannels.get(id)) {
            int[] subs = this.addMan.getSubnetMap();
            if (subs != null) {
               for (int i = 0; i < subs.length; i++) {
                  if (subs[i] == id) {
                     this.clearFlag(i, a);
                  }
               }
            }
         }
      }

      return BLonRouteTable.make(a);
   }

   private byte[] newRouteTableArray() {
      return BLonRouteTable.DEFAULT.getByteArrayCopy();
   }

   private void clearFlag(int index, byte[] a) {
      int byteOffset = index / 8;
      int bitOffset = index % 8;
      a[byteOffset] = (byte)(a[byteOffset] & ~(1 << bitOffset));
   }

   private RouterManager.Router getRouter(BLonRouter rtr) {
      int farChan = rtr.getFarDeviceData().getChannelId();

      for (int i = 0; i < this.rtrTab.length; i++) {
         if (this.rtrTab[i].farChan == farChan) {
            return this.rtrTab[i];
         }
      }

      return null;
   }

   public void verifySubnets() {
      BLonRouter[] routers = this.addMan.getRouterList();

      for (int i = 0; i < routers.length; i++) {
         boolean changed = false;
         BLonRouter router = routers[i];
         BLonRouteTable newFarTab = this.getFarRouteTable(this.rtrTab[i]);

         try {
            BLonRouteTable origTab = router.getFarSubnetTable();
            if (!origTab.equals(newFarTab)) {
               RouterUtil.updateTable(router, 0, true, newFarTab, origTab);
               router.setFarSubnetTable(newFarTab);
               changed = true;
            }

            BLonRouteTable newNearTab = newFarTab.invert(this.addMan.getSubnetMap());
            origTab = router.getNearSubnetTable();
            if (!origTab.equals(newNearTab)) {
               RouterUtil.updateTable(router, 0, false, newNearTab, origTab);
               router.setNearSubnetTable(newNearTab);
               changed = true;
            }

            if (changed && router.getRouterType() == BLonRouterType.configured) {
               BLonRouterMode mode = router.getRouterMode();
               if (mode == BLonRouterMode.normal) {
                  RouterUtil.setRouterMode(router, BLonRouterMode.initRouterTable);
                  if (this.lon.netmgmt().getTempBridge()) {
                     RouterUtil.setRouterMode(router, BLonRouterMode.normal);
                  }
               }
            }
         } catch (LonException var9) {
            this.lon.log().severe("Unable to update subnet route table in " + router.getDisplayName(null));
         }
      }
   }

   public NAddressManager getAddressManager() {
      return this.addMan;
   }

   public void initGroupRouteUpdate() {
      for (int i = 0; i < this.rtrTab.length; i++) {
         this.rtrTab[i].nearGroupTable = this.newRouteTableArray();
         this.rtrTab[i].farGroupTable = this.newRouteTableArray();
      }
   }

   public void setGroupRouteFlags(int groupNum, BitSet groupChannels) {
      for (int i = 0; i < this.rtrTab.length; i++) {
         BitSet farChan = this.rtrTab[i].farChannels;
         boolean onFarSide = false;
         boolean onNearSide = false;

         for (int id = 1; id <= this.maxChanId; id++) {
            if (groupChannels.get(id)) {
               if (farChan.get(id)) {
                  onFarSide = true;
               } else {
                  onNearSide = true;
               }
            }
         }

         if (onFarSide && !onNearSide) {
            this.clearFlag(groupNum, this.rtrTab[i].farGroupTable);
         } else if (onNearSide && !onFarSide) {
            this.clearFlag(groupNum, this.rtrTab[i].nearGroupTable);
         }
      }
   }

   public void updateGroupRouteFlags() {
      BLonRouter[] routers = this.addMan.getRouterList();

      for (int i = 0; i < routers.length; i++) {
         BLonRouter router = routers[i];

         try {
            boolean update = false;
            BLonRouteTable newTab = BLonRouteTable.make(this.rtrTab[i].nearGroupTable);
            BLonRouteTable origTab = router.getNearGroupTable();
            if (!origTab.equals(newTab)) {
               RouterUtil.updateTable(router, 1, false, newTab, origTab);
               router.setNearGroupTable(newTab);
               update = true;
            }

            newTab = BLonRouteTable.make(this.rtrTab[i].farGroupTable);
            origTab = router.getFarGroupTable();
            if (!origTab.equals(newTab)) {
               RouterUtil.updateTable(router, 1, true, newTab, origTab);
               router.setFarGroupTable(newTab);
               update = true;
            }

            if (update && router.getRouterType() == BLonRouterType.configured) {
               BLonRouterMode mode = router.getRouterMode();
               if (mode == BLonRouterMode.normal) {
                  RouterUtil.setRouterMode(router, BLonRouterMode.initRouterTable);
                  if (this.lon.netmgmt().getTempBridge()) {
                     RouterUtil.setRouterMode(router, BLonRouterMode.normal);
                  }
               }
            }
         } catch (LonException var8) {
            this.lon.log().severe("Unable to update group route table in " + router.getDisplayName(null));
         }
      }
   }

   public void spy(SpyWriter out) throws Exception {
      BLonRouter[] routers = this.addMan.getRouterList();
      out.startProps("Router table");

      for (int i = 0; i < this.rtrTab.length; i++) {
         RouterManager.Router r = this.rtrTab[i];
         out.prop(routers[i].getName(), "");
         out.prop("nearChan", Integer.toString(r.nearChan));
         out.prop("farChan", Integer.toString(r.farChan));
         out.prop("farChannels", r.farChannels.toString());
         out.prop("nearGroupTable", LonByteArrayUtil.toString(r.nearGroupTable));
         out.prop("farGroupTable", LonByteArrayUtil.toString(r.farGroupTable));
      }

      out.endProps();
   }

   public static class Router {
      int nearChan;
      int farChan;
      BitSet farChannels;
      byte[] nearGroupTable;
      byte[] farGroupTable;
   }
}
