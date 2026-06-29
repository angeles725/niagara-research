package com.tridium.lonworks;

import com.tridium.lonworks.util.NmUtil;
import java.util.Vector;
import javax.baja.lonworks.AddressManager;
import javax.baja.lonworks.BLocalLonDevice;
import javax.baja.lonworks.BLonDevice;
import javax.baja.lonworks.BLonNetwork;
import javax.baja.lonworks.LonException;
import javax.baja.lonworks.datatypes.BDeviceData;
import javax.baja.lonworks.datatypes.BNeuronId;
import javax.baja.lonworks.datatypes.BSubnetNode;
import javax.baja.lonworks.enums.BLonNodeState;
import javax.baja.nre.util.IntHashMap;
import javax.baja.nre.util.IntHashMap.Iterator;
import javax.baja.spy.SpyWriter;
import javax.baja.status.BStatus;
import javax.baja.sys.BComplex;
import javax.baja.sys.BComponent;
import javax.baja.sys.BString;
import javax.baja.sys.BasicContext;
import javax.baja.sys.Context;

public class NAddressManager implements AddressManager {
   private static final int SUBNET_NODE_MASK = 32767;
   private boolean updateRouterManager = true;
   private RouterManager routerManager = null;
   public static final int MAX_NODEID = 127;
   public static final int FIRST_SUBNET = 1;
   public static final int MAX_SUBNET = 255;
   public static final int DEFAULT_LON_NODE = 0;
   public static final int UNASSIGNED_CHANNEL_ID = 0;
   public static final int FIRST_CHANNEL_ID = 1;
   public static final int MAX_CHANNEL_ID = 65535;
   public static final int DEFAULT_CHANNEL_ID = 1;
   public static final Context localChange = new BasicContext();
   private IntHashMap deviceHash = new IntHashMap(127);
   private int[] subnetMap;
   private Vector<BLonDevice> devVect = new Vector<>(200);
   private BLocalLonDevice local = null;
   private BLonDevice[] devList = null;
   private boolean staleDeviceList = true;
   private Vector<BLonRouter> rtrVect = new Vector<>(200);
   private BLonRouter[] rtrList = null;
   private boolean staleRtrList = true;
   private BLonNetwork lon;

   public NAddressManager(BLonNetwork lonworks) {
      this.lon = lonworks;
      this.subnetMap = new int[256];

      for (int i = 0; i < 256; i++) {
         this.subnetMap[i] = 0;
      }
   }

   @Override
   public void registerLonDevice(BLonDevice dev) {
      try {
         synchronized (this.deviceHash) {
            this.registerAddress(dev.getDeviceData(), dev, 0);
            if (dev.isLocal()) {
               this.local = (BLocalLonDevice)dev;
            } else {
               this.devVect.addElement(dev);
            }

            this.staleDeviceList = true;
         }

         this.lon.log().fine("registerLonDevice " + dev.getDisplayName(null) + " " + Integer.toString(dev.getDeviceData().getLastHash(), 16));
         this.lon.fireDeviceChange(null);
         NmUtil.validateNeuronId(dev.getDeviceData().getNeuronId(), dev);
         dev.configOk();
      } catch (Throwable var5) {
         dev.configFail(var5.getMessage());
      }
   }

   @Override
   public void unregisterLonDevice(BLonDevice dev) {
      synchronized (this.deviceHash) {
         if (dev.isLocal()) {
            this.local = null;
         } else {
            this.devVect.removeElement(dev);
         }

         this.unregisterAddress(dev.getDeviceData());
         this.staleDeviceList = true;
      }

      this.lon.log().fine("unregisterLonDevice " + dev.getDisplayName(null) + " " + Integer.toString(dev.getDeviceData().getLastHash(), 16));
      this.lon.fireDeviceChange(null);
   }

   public void registerLonRouter(BLonRouter rtr) {
      try {
         synchronized (this.deviceHash) {
            this.registerAddress(rtr.getNearDeviceData(), rtr, 0);

            try {
               this.registerAddress(rtr.getFarDeviceData(), rtr, 1);
            } catch (Throwable var5) {
               this.unregisterAddress(rtr.getNearDeviceData());
               throw var5;
            }

            this.rtrVect.addElement(rtr);
            this.staleRtrList = true;
            this.updateRouterManager = true;
         }

         this.lon
            .log()
            .fine(
               "registerLonRouter "
                  + rtr.getDisplayName(null)
                  + " "
                  + Integer.toString(rtr.getNearDeviceData().getLastHash(), 16)
                  + " "
                  + Integer.toString(rtr.getFarDeviceData().getLastHash(), 16)
            );
         this.lon.fireDeviceChange(null);
         NmUtil.validateNeuronId(rtr.getNearDeviceData().getNeuronId(), rtr);
         rtr.setStatus(BStatus.ok);
         rtr.setFaultCause("");
      } catch (Throwable var7) {
         rtr.setStatus(BStatus.fault);
         rtr.setFaultCause(var7.getMessage());
      }
   }

   public void unregisterLonRouter(BLonRouter rtr) {
      synchronized (this.deviceHash) {
         this.rtrVect.removeElement(rtr);
         this.unregisterAddress(rtr.getNearDeviceData());
         this.unregisterAddress(rtr.getFarDeviceData());
         this.staleRtrList = true;
         this.updateRouterManager = true;
      }

      this.lon
         .log()
         .fine(
            "unregisterLonRouter "
               + rtr.getDisplayName(null)
               + " "
               + Integer.toString(rtr.getNearDeviceData().getLastHash(), 16)
               + " "
               + Integer.toString(rtr.getFarDeviceData().getLastHash(), 16)
         );
      this.lon.fireDeviceChange(null);
   }

   private void registerAddress(BDeviceData dd, BComponent dev, int delta) throws LonException {
      if (dd.getChannelId() == 0) {
         dd.setInt(BDeviceData.channelId, 1 + delta, noDeviceChange);
      }

      int hash = getDeviceHash(dd);
      if ((hash & 32767) == 0) {
         hash = this.claimSubnetNodeId(dd);
      }

      if (!this.verifySubnetChannel(hash)) {
         String msg = dev.getDisplayName(null)
            + " has invalid subnet "
            + getSubnetFromHash(hash)
            + " for channel "
            + getChanIdFromHash(hash)
            + ". "
            + dev.getDisplayName(null)
            + " not registered.";
         this.lon.log().severe(msg);
         throw new LonException(msg);
      } else {
         BComplex o;
         if ((o = (BComplex)this.deviceHash.get(hash)) != null) {
            if (o != dev) {
               String msg = "Duplicate nodeId detected with "
                  + o.getDisplayName(null)
                  + ". "
                  + dev.getDisplayName(null)
                  + " not registered. {"
                  + Integer.toString(hash, 16)
                  + "}";
               this.lon.log().severe(msg);
               throw new LonException(msg);
            }
         } else {
            this.deviceHash.put(hash, dev);
            dd.setInt(BDeviceData.lastHash, hash, localChange);
         }
      }
   }

   private void unregisterAddress(BDeviceData dd) {
      int hash = dd.getLastHash();
      this.deviceHash.remove(hash);
      this.checkFreeSubnet(hash);
      dd.setInt(BDeviceData.lastHash, -1, localChange);
   }

   @Override
   public void deviceDataChanged(BDeviceData dd, Context context) {
      if (context != localChange) {
         int lastHash = dd.getLastHash();
         if (lastHash != -1 && lastHash == getDeviceHash(dd)) {
            this.validateNeuronId(dd);
         } else {
            BComplex o = dd.getParent();
            if (o instanceof BLonDevice) {
               BLonDevice dev = (BLonDevice)o;
               this.unregisterLonDevice(dev);
               this.registerLonDevice(dev);
            } else if (o instanceof BLonRouter) {
               BLonRouter rtr = (BLonRouter)o;
               this.unregisterLonRouter(rtr);
               this.registerLonRouter(rtr);
            }
         }

         if (context != noDeviceChange) {
            this.lon.fireDeviceChange(null);
         }
      }
   }

   private boolean validateNeuronId(BDeviceData dd) {
      BComplex p = dd.getParent();
      String cause = "";
      boolean valid = true;

      try {
         NmUtil.validateNeuronId(dd.getNeuronId(), (BComponent)p);
      } catch (LonException var6) {
         cause = var6.getMessage();
         valid = false;
      }

      if (p instanceof BLonRouter) {
         BLonRouter rtr = (BLonRouter)p;
         if (!valid) {
            rtr.setFaultCause(cause);
            rtr.setStatus(BStatus.make(rtr.getStatus().getBits() | 2));
         } else if (rtr.getFaultCause().indexOf("NeuronId") >= 0) {
            rtr.setFaultCause("");
            rtr.setStatus(BStatus.make(rtr.getStatus().getBits() & -3));
         }
      } else if (p instanceof BLonDevice) {
         BLonDevice dev = (BLonDevice)p;
         if (!valid) {
            dev.configFail(cause);
         } else if (dev.getFaultCause().indexOf("NeuronId") >= 0) {
            dev.configOk();
         }
      }

      return valid;
   }

   @Override
   public BString newAddress(BLonDevice dev, int chan, int subnet, int node) {
      BDeviceData dd = dev.getDeviceData();
      int lastHash = dd.getLastHash();
      synchronized (this.deviceHash) {
         if (!this.isValidSubnet(subnet, chan)) {
            if (chan == getChanIdFromHash(lastHash)) {
               return BString.make("Invalid Subnet for channel id.");
            }

            int h = this.findSubnetNode(chan);
            subnet = getSubnetFromHash(h);
            node = getNodeFromHash(h);
         }

         if (!this.isAddressAvailable(subnet, node, chan)) {
            return BString.make("Subnet node " + subnet + "\\" + node + " is already inuse.");
         } else {
            this.unregisterLonDevice(dev);
            dd.set(BDeviceData.subnetNodeId, BSubnetNode.make(subnet, node), localChange);
            dd.setInt(BDeviceData.channelId, chan, localChange);
            if (dev.isConfigured()) {
               dd.set(BDeviceData.nodeState, BLonNodeState.unconfigured, localChange);
            }

            this.registerLonDevice(dev);
            return null;
         }
      }
   }

   public BString newAddress(BLonRouter rtr, int nearChan, BSubnetNode nearAddr, int farChan, BSubnetNode farAddr) {
      BDeviceData ndd = rtr.getNearDeviceData();
      BDeviceData fdd = rtr.getFarDeviceData();
      int nearSubnet = nearAddr.getSubnetId();
      int nearNode = nearAddr.getNodeId();
      int farSubnet = farAddr.getSubnetId();
      int farNode = farAddr.getNodeId();
      synchronized (this.deviceHash) {
         if (nearChan == farChan) {
            return BString.make("Near and far channelIds must be different.");
         } else {
            if (!this.isValidSubnet(nearSubnet, nearChan)) {
               int lastHash = ndd.getLastHash();
               if (nearChan == getChanIdFromHash(lastHash)) {
                  return BString.make("Subnet " + nearSubnet + " is invalid  for channel " + nearChan + ".");
               }

               int h = this.findSubnetNode(nearChan);
               nearSubnet = getSubnetFromHash(h);
               nearNode = getNodeFromHash(h);
            }

            if (!this.isValidSubnet(farSubnet, farChan)) {
               int lastHash = fdd.getLastHash();
               if (farChan == getChanIdFromHash(lastHash)) {
                  return BString.make("Subnet " + farSubnet + " is invalid  for channel " + farChan + ".");
               }

               int h = this.findSubnetNode(farChan);
               farSubnet = getSubnetFromHash(h);
               farNode = getNodeFromHash(h);
            }

            if (!this.isAddressAvailable(nearSubnet, nearNode, nearChan)) {
               return BString.make("Subnet node " + nearSubnet + "\\" + nearNode + " is already inuse.");
            } else if (!this.isAddressAvailable(farSubnet, farNode, farChan)) {
               return BString.make("Subnet node " + farSubnet + "\\" + farNode + " is already inuse.");
            } else {
               this.unregisterLonRouter(rtr);
               ndd.set(BDeviceData.subnetNodeId, BSubnetNode.make(nearSubnet, nearNode), localChange);
               ndd.setInt(BDeviceData.channelId, nearChan, localChange);
               fdd.set(BDeviceData.subnetNodeId, BSubnetNode.make(farSubnet, farNode), localChange);
               fdd.setInt(BDeviceData.channelId, farChan, localChange);
               if (rtr.isConfigured()) {
                  ndd.set(BDeviceData.nodeState, BLonNodeState.unconfigured, localChange);
                  fdd.set(BDeviceData.nodeState, BLonNodeState.unconfigured, localChange);
               }

               this.registerLonRouter(rtr);
               return null;
            }
         }
      }
   }

   private boolean isAddressAvailable(int subnet, int node, int chan) {
      int hash = getDeviceHash(subnet, node, chan);
      Object o = this.deviceHash.get(hash);
      if (o == null) {
         return true;
      } else {
         if (o instanceof BLonDevice) {
            BLonDevice d = (BLonDevice)o;
            if (d.isConfigured()) {
               return false;
            }

            this.claimSubnetNodeId(d.getDeviceData());
            this.unregisterLonDevice(d);
            this.registerLonDevice(d);
         } else if (o instanceof BLonRouter) {
            BLonRouter r = (BLonRouter)o;
            if (r.isConfigured()) {
               return false;
            }

            if (r.getNearDeviceData().getLastHash() == hash) {
               this.claimSubnetNodeId(r.getNearDeviceData());
            } else if (r.getFarDeviceData().getLastHash() == hash) {
               this.claimSubnetNodeId(r.getFarDeviceData());
            }

            this.unregisterLonRouter(r);
            this.registerLonRouter(r);
         }

         return true;
      }
   }

   @Override
   public BLonDevice[] getDeviceList(boolean includeLocal) {
      synchronized (this.deviceHash) {
         BLonDevice[] s = this.makeDeviceList();
         BLonDevice[] d = new BLonDevice[s.length + (includeLocal ? 1 : 0)];
         if (includeLocal) {
            d[0] = this.local;
            System.arraycopy(s, 0, d, 1, s.length);
         } else {
            System.arraycopy(s, 0, d, 0, s.length);
         }

         return d;
      }
   }

   private BLonDevice[] makeDeviceList() {
      synchronized (this.deviceHash) {
         if (this.staleDeviceList) {
            this.devList = new BLonDevice[this.devVect.size()];
            this.devVect.copyInto(this.devList);
            this.orderList(this.devList);
            this.staleDeviceList = false;
         }

         return this.devList;
      }
   }

   private void orderList(BLonDevice[] list) {
      int firstNdx = 0;
      int lastNdx = list.length - 1;
      int i = firstNdx;

      while (i < lastNdx) {
         if (getDeviceHash(list[i]) > getDeviceHash(list[i + 1])) {
            BLonDevice tmp = list[i];
            list[i] = list[i + 1];
            list[i + 1] = tmp;
            if (i > firstNdx) {
               i--;
            } else {
               i++;
            }
         } else {
            i++;
         }
      }
   }

   private void orderList(BLonRouter[] list) {
      int firstNdx = 0;
      int lastNdx = list.length - 1;
      int i = firstNdx;

      while (i < lastNdx) {
         if (getDeviceHash(list[i].getNearDeviceData()) > getDeviceHash(list[i + 1].getNearDeviceData())) {
            BLonRouter tmp = list[i];
            list[i] = list[i + 1];
            list[i + 1] = tmp;
            if (i > firstNdx) {
               i--;
            } else {
               i++;
            }
         } else {
            i++;
         }
      }
   }

   @Override
   public BLonDevice getDeviceByName(String name) {
      if (this.local != null && this.local.getName().equals(name)) {
         return this.local;
      } else {
         BLonDevice[] devList = this.makeDeviceList();

         for (int i = 0; i < devList.length; i++) {
            if (devList[i].getName().equals(name)) {
               return devList[i];
            }
         }

         return null;
      }
   }

   @Override
   public BLonDevice getDeviceByAddress(BSubnetNode addr) {
      if (this.local != null && this.local.getDeviceData().getSubnetNodeId().equals(addr)) {
         return this.local;
      } else {
         BLonDevice[] devList = this.makeDeviceList();

         for (int i = 0; i < devList.length; i++) {
            if (devList[i].getDeviceData().getSubnetNodeId().equals(addr)) {
               return devList[i];
            }
         }

         return null;
      }
   }

   @Override
   public BLonDevice getDeviceByAddress(BNeuronId nid) {
      if (this.local != null && this.local.getDeviceData().getNeuronId().equals(nid)) {
         return this.local;
      } else {
         BLonDevice[] devList = this.makeDeviceList();

         for (int i = 0; i < devList.length; i++) {
            if (devList[i].getDeviceData().getNeuronId().equals(nid)) {
               return devList[i];
            }
         }

         return null;
      }
   }

   public BLonRouter getRouterByName(String name) {
      BLonRouter[] rtrList = this.makeRouterList();

      for (int i = 0; i < rtrList.length; i++) {
         if (rtrList[i].getName().equals(name)) {
            return rtrList[i];
         }
      }

      return null;
   }

   @Override
   public BLonRouter getRouterByAddress(BSubnetNode addr) {
      BLonRouter[] rtrList = this.makeRouterList();

      for (int i = 0; i < rtrList.length; i++) {
         BLonRouter rtr = rtrList[i];
         if (rtr.getNearDeviceData().getSubnetNodeId().equals(addr) || rtr.getFarDeviceData().getSubnetNodeId().equals(addr)) {
            return rtr;
         }
      }

      return null;
   }

   @Override
   public BLonRouter getRouterByAddress(BNeuronId nid) {
      BLonRouter[] rtrList = this.makeRouterList();

      for (int i = 0; i < rtrList.length; i++) {
         BLonRouter rtr = rtrList[i];
         if (rtr.getNearDeviceData().getNeuronId().equals(nid) || rtr.getFarDeviceData().getNeuronId().equals(nid)) {
            return rtr;
         }
      }

      return null;
   }

   @Override
   public BLocalLonDevice getLocalDevice() {
      return this.local;
   }

   @Override
   public BLonRouter[] getRouterList() {
      synchronized (this.deviceHash) {
         BLonRouter[] s = this.makeRouterList();
         BLonRouter[] d = new BLonRouter[s.length];
         System.arraycopy(s, 0, d, 0, s.length);
         return d;
      }
   }

   public boolean isRouted() {
      return this.makeRouterList().length > 0;
   }

   protected int[] getSubnetMap() {
      return this.subnetMap;
   }

   public int getMaxChannelId() {
      synchronized (this.deviceHash) {
         int maxId = 1;

         for (int i = 0; i < this.subnetMap.length; i++) {
            if (this.subnetMap[i] > maxId) {
               maxId = this.subnetMap[i];
            }
         }

         return maxId;
      }
   }

   private BLonRouter[] makeRouterList() {
      synchronized (this.deviceHash) {
         if (this.staleRtrList) {
            this.rtrList = new BLonRouter[this.rtrVect.size()];
            this.rtrVect.copyInto(this.rtrList);
            this.orderList(this.rtrList);
            this.staleRtrList = false;
         }

         return this.rtrList;
      }
   }

   private boolean verifySubnetChannel(int hash) {
      int sub = getSubnetFromHash(hash);
      int chan = getChanIdFromHash(hash);
      int s = this.subnetMap[sub];
      if (s == chan) {
         return true;
      } else if (s == 0) {
         this.subnetMap[sub] = chan;
         this.updateRouterManager = true;
         return true;
      } else {
         return false;
      }
   }

   private void checkFreeSubnet(int hash) {
      int sub = getSubnetFromHash(hash);
      if (this.local == null || !this.isAnotherWithSameSubnet(sub, hash, this.local.getDeviceData())) {
         BLonDevice[] a = this.makeDeviceList();

         for (int i = 0; i < a.length; i++) {
            if (this.isAnotherWithSameSubnet(sub, hash, a[i].getDeviceData())) {
               return;
            }
         }

         BLonRouter[] r = this.makeRouterList();

         for (int ix = 0; ix < r.length; ix++) {
            if (this.isAnotherWithSameSubnet(sub, hash, r[ix].getNearDeviceData())) {
               return;
            }

            if (this.isAnotherWithSameSubnet(sub, hash, r[ix].getFarDeviceData())) {
               return;
            }
         }

         this.lon.log().fine("free subnet " + sub);
         this.subnetMap[sub] = 0;
         this.updateRouterManager = true;
      }
   }

   private boolean isAnotherWithSameSubnet(int sub, int hash, BDeviceData dd) {
      return dd.getLastHash() == hash ? false : dd.getSubnetNodeId().getSubnetId() == sub;
   }

   private boolean isValidSubnet(int subnetId, int channelId) {
      int c = this.subnetMap[subnetId];
      return c == channelId || c == 0;
   }

   private int claimSubnetNodeId(BDeviceData dd) {
      int chanId = dd.getChannelId();
      int hash = this.findSubnetNode(chanId);
      BSubnetNode sn = BSubnetNode.make(getSubnetFromHash(hash), getNodeFromHash(hash));
      dd.set(BDeviceData.subnetNodeId, sn, localChange);
      return hash;
   }

   private int findSubnetNode(int chanId) {
      int subnet = this.getNextSubnet(0, chanId);
      int nodeId = 1;

      while (this.deviceHash.get(getDeviceHash(subnet, nodeId, chanId)) != null) {
         if (++nodeId >= 127) {
            subnet = this.getNextSubnet(subnet, chanId);
            nodeId = 1;
         }
      }

      return getDeviceHash(subnet, nodeId, chanId);
   }

   private int getNextSubnet(int orig, int chanId) {
      for (int i = ++orig; i <= 255; i++) {
         if (this.subnetMap[i] == chanId) {
            return i;
         }
      }

      int firstFree = -1;

      for (int ix = chanId; ix <= 255; ix++) {
         if (this.subnetMap[ix] == 0) {
            this.lon.log().fine("claim subnet " + ix + " for channel " + chanId);
            this.subnetMap[ix] = chanId;
            this.updateRouterManager = true;
            return ix;
         }
      }

      throw new RuntimeException("No more available subnets.");
   }

   public static int getDeviceHash(BLonDevice lonDevice) {
      return getDeviceHash(lonDevice.getDeviceData());
   }

   private static int getDeviceHash(BDeviceData dd) {
      BSubnetNode sn = dd.getSubnetNodeId();
      return getDeviceHash(sn.getSubnetId(), sn.getNodeId(), dd.getChannelId());
   }

   private static int getDeviceHash(int subnet, int node, int chanId) {
      return (chanId << 15) + (subnet << 7) + node;
   }

   public static int getSubnetFromHash(int hash) {
      return hash >> 7 & 0xFF;
   }

   public static int getNodeFromHash(int hash) {
      return hash & 127;
   }

   public static int getChanIdFromHash(int hash) {
      return hash >> 15 & 65535;
   }

   public RouterManager routerManager() {
      if (this.routerManager == null) {
         this.routerManager = new RouterManager(this, this.lon);
      }

      if (this.updateRouterManager) {
         this.routerManager.update();
      }

      this.updateRouterManager = false;
      return this.routerManager;
   }

   public void spy(SpyWriter out) throws Exception {
      out.startTable(true);
      out.trTitle("subnetMap", 2);
      out.w("<tr>").th("Subnet").th("Channel").w("</tr>\n");

      for (int i = 0; i < this.subnetMap.length; i++) {
         if (this.subnetMap[i] != 0) {
            out.tr(Integer.toString(i), Integer.toString(this.subnetMap[i]));
         }
      }

      out.endTable();
      out.startProps("Misc");
      out.prop("updateRouterManager", this.updateRouterManager);
      out.prop("staleDeviceList", this.staleDeviceList);
      out.prop("staleRtrList", this.staleRtrList);
      out.prop("local", this.local.getName());
      out.endProps();
      out.startTable(true);
      out.trTitle("deviceHash", 4);
      out.w("<tr>").th("dev").th("chan").th("s/n").th("hash").w("</tr>\n");
      Iterator it = this.deviceHash.iterator();

      while (it.hasNext()) {
         BComponent c = (BComponent)it.next();
         int h = it.key();
         out.tr(
            c.getName(),
            Integer.toString(getChanIdFromHash(h)),
            Integer.toString(getSubnetFromHash(h)) + "/" + Integer.toString(getNodeFromHash(h)),
            Integer.toString(h, 16)
         );
      }

      out.endTable();
      if (this.devList != null) {
         out.startProps("devList");

         for (int ix = 0; ix < this.devList.length; ix++) {
            if (this.devList[ix] != null) {
               out.prop(Integer.toString(ix), this.devList[ix].getName());
            }
         }

         out.endProps();
      }

      if (this.rtrList != null) {
         out.startProps("rtrList");

         for (int ixx = 0; ixx < this.rtrList.length; ixx++) {
            if (this.rtrList[ixx] != null) {
               out.prop(Integer.toString(ixx), this.rtrList[ixx].getName());
            }
         }

         out.endProps();
      }

      this.routerManager().spy(out);
   }
}
