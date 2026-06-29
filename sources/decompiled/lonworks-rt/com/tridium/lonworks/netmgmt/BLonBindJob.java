package com.tridium.lonworks.netmgmt;

import com.tridium.lonworks.Lon;
import com.tridium.lonworks.NAddressManager;
import com.tridium.lonworks.datatypes.BTagLinkEntry;
import com.tridium.lonworks.enums.BLonLinkStatus;
import com.tridium.lonworks.util.Neuron;
import com.tridium.lonworks.util.NmUtil;
import java.util.StringTokenizer;
import java.util.Vector;
import javax.baja.job.JobCancelException;
import javax.baja.lonworks.AddressManager;
import javax.baja.lonworks.BINetworkVariable;
import javax.baja.lonworks.BLonDevice;
import javax.baja.lonworks.BLonNetwork;
import javax.baja.lonworks.BNetworkVariable;
import javax.baja.lonworks.LonException;
import javax.baja.lonworks.datatypes.BAddressEntry;
import javax.baja.lonworks.datatypes.BAliasConfigData;
import javax.baja.lonworks.datatypes.BAliasTable;
import javax.baja.lonworks.datatypes.BDeviceData;
import javax.baja.lonworks.datatypes.BIAddressEntry;
import javax.baja.lonworks.datatypes.BNvConfigData;
import javax.baja.lonworks.datatypes.BNvProps;
import javax.baja.lonworks.enums.BLonLinkType;
import javax.baja.lonworks.enums.BLonNodeState;
import javax.baja.naming.SlotPath;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BString;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public class BLonBindJob extends BLonNetmgmtJob implements NetMgmtConst {
   public static final Type TYPE = Sys.loadType(BLonBindJob.class);
   private int completes = 0;
   private ConnectionTable connTable;
   private Connection[] table;
   private TagConnection[] tags;
   private NAddressManager adrMan;
   private GroupTable groupTable;
   private boolean debug;
   private BTagLinkEntry tagEntry = null;
   private BLonDevice selectDev = null;
   boolean bindMtags = true;
   Vector<BLonDevice> offDevs = new Vector<>();

   @Override
   public Type getType() {
      return TYPE;
   }

   public BLonBindJob() {
   }

   public BLonBindJob(BLonNetmgmt netMgmt, BString selects, BTagLinkEntry tagEntry, boolean debug) {
      super(netMgmt);
      this.connTable = new ConnectionTable(netMgmt.lonNetwork());
      this.tags = this.connTable.getMessageTagTable();
      this.groupTable = this.connTable.getGroupTable();
      this.tagEntry = tagEntry;
      this.debug = debug;
      this.adrMan = (NAddressManager)((BLonNetwork)netMgmt.getParent()).addressManager();
      this.groupTable.bindJob = this;
      if (tagEntry != null) {
         this.table = new Connection[0];
      } else {
         this.table = this.connTable.getConnectionArray();
         if (selects != null) {
            String s = selects.getString();
            if (s.length() > 0) {
               this.bindMtags = false;
               StringTokenizer st = new StringTokenizer(s, ",");
               Vector<Connection> v = new Vector<>();

               while (st.hasMoreTokens()) {
                  int sel = Integer.decode(st.nextToken());
                  v.addElement(this.table[sel]);
               }

               this.table = new Connection[v.size()];
               v.copyInto(this.table);
            }
         }
      }
   }

   public BLonBindJob(BLonNetmgmt netMgmt, BLonDevice dev, boolean debug) {
      this(netMgmt, dev, true, debug);
   }

   public BLonBindJob(BLonNetmgmt netMgmt, BLonDevice dev, boolean bindMtags, boolean debug) {
      super(netMgmt);
      this.connTable = new ConnectionTable(netMgmt.lonNetwork());
      this.tags = this.connTable.getMessageTagTable();
      this.groupTable = this.connTable.getGroupTable();
      this.selectDev = dev;
      this.debug = debug;
      this.adrMan = (NAddressManager)((BLonNetwork)netMgmt.getParent()).addressManager();
      this.groupTable.bindJob = this;
      this.table = this.connTable.getConnectionArray();
      Vector<Connection> v = new Vector<>();

      for (int i = 0; i < this.table.length; i++) {
         if (this.table[i] != null && this.table[i].containsDevice(dev)) {
            v.addElement(this.table[i]);
         }
      }

      this.table = new Connection[v.size()];
      v.copyInto(this.table);
   }

   static void bindDevice(BLonNetmgmt netMgmt, BLonDevice dev) {
      BLonBindJob req = new BLonBindJob(netMgmt, dev, false);
      req.run();
   }

   @Override
   public void run() {
      this.log().start("Binding");
      if (this.debug) {
         System.out.println("Binding");
      }

      try {
         if (this.bindMtags) {
            this.processMessageTags();
         }

         this.myProgress(5);
         this.selectGroup();
         this.myProgress(10);
         this.status("Binding - processing groups");
         this.groupTable.processGroups(this.offDevs);
         this.myProgress(30);
         this.status("Binding - processing links");
         this.processConnections();
         this.myProgress(80);
         this.removeObsolete();
         this.verifyChannelPriorities();
         this.updateTagStatus();
         if (this.adrMan.isRouted() && Lon.n()) {
            this.groupTable.updateGroupRouteFlags(this.adrMan.routerManager());
         }

         this.setDevicesOnline();
         this.end();
      } catch (JobCancelException var2) {
         this.setDevicesOnline();
      } catch (Throwable var3) {
         var3.printStackTrace();
         this.fatal("Bind command failed.  ", var3);
      }

      this.netMgmt.updateLinkTable(this.connTable);
      if (this.debug) {
         System.out.println("Bind complete");
      }
   }

   private void setDevicesOnline() {
      for (int i = 0; i < this.offDevs.size(); i++) {
         BLonDevice dev = this.offDevs.elementAt(i);

         try {
            if (!dev.isConfigOnline()) {
               if (Lon.n()) {
                  NmUtil.setDeviceState(dev, BLonNodeState.configOnline);
               }

               dev.getDeviceData().set(BDeviceData.nodeState, BLonNodeState.configOnline, AddressManager.noDeviceChange);
            }

            try {
               dev.bindComplete();
            } catch (Throwable var4) {
            }
         } catch (Throwable var5) {
            System.out.println("ERROR: in GroupTable.setDevicesOnline() - comm failure to " + dev.getDisplayName(null));
         }
      }
   }

   private void updateTagStatus() {
      for (int i = 0; i < this.tags.length; i++) {
         TagConnection tagCnctn = this.tags[i];
         if (!tagCnctn.isError()) {
            int groupNum = tagCnctn.getAddressGroup();
            if (groupNum != -1) {
               Group grp = this.groupTable.getGroup(groupNum);
               if (grp != null && grp.isUpdated()) {
                  tagCnctn.setUpdatedStatus();
               }
            }
         }
      }
   }

   private void processMessageTags() {
      for (int i = 0; i < this.tags.length; i++) {
         TagConnection tagCnctn = this.tags[i];
         TagPoint out = tagCnctn.getOutput();
         if (this.tagEntry != null) {
            if (out.getDeviceName().equals(SlotPath.unescape(this.tagEntry.getOutputDevice()))
               && out.getTagName().equals(SlotPath.unescape(this.tagEntry.getOutputTag()))) {
               this.processTagConnection(tagCnctn);
               return;
            }
         } else if (this.selectDev != null) {
            if (tagCnctn.containsDevice(this.selectDev)) {
               this.processTagConnection(tagCnctn);
            }
         } else {
            this.processTagConnection(tagCnctn);
         }
      }
   }

   private void processTagConnection(TagConnection tagCnctn) {
      if (this.debug) {
         System.out.println("processTagConnection: " + tagCnctn);
      }

      TagPoint out = tagCnctn.getOutput();
      if (out != null) {
         int grpNum = tagCnctn.getAddressGroup();
         Group grp = grpNum >= 0 ? this.groupTable.getGroup(grpNum) : null;
         BLonDevice outDev = out.getLonDevice();
         if (out.isObsolete()) {
            if (grp != null) {
               grp.removeMember(this.connTable.getDeviceIndex(outDev));
            }

            this.groupTable.clearAddressEntry(outDev, out.getTagIndex(), this.offDevs);
            tagCnctn.setStatus(BLonLinkStatus.unbound);
            tagCnctn.setAddressGroup(-1);
         } else {
            boolean drtyDsc = out.getStatus() == BLonLinkStatus.dirtyDescriptor;
            if (drtyDsc && grp != null) {
               grp.setLinkType(BLonLinkType.reliable);
               grp.setStatus(1);
               tagCnctn.setStatus(BLonLinkStatus.bound);
            } else if (drtyDsc || !tagCnctn.isError() && tagCnctn.isNew()) {
               TagPoint[] ins = tagCnctn.getInputs();
               if (ins.length > 1 && grpNum >= 0) {
                  if (this.connTable.groupUse(grpNum, true) <= 1 && this.groupTable.reconfigTagGroup(tagCnctn, grpNum, this.offDevs)) {
                     return;
                  }

                  if (!this.groupTable.moveTagPoint(out, grpNum, this.offDevs)) {
                     tagCnctn.setStatus(BLonLinkStatus.groupError);
                     return;
                  }

                  grp = null;
                  int var12 = -1;
                  tagCnctn.setAddressGroup(-1);
               }

               if (ins.length == 1 && grp != null && !ins[0].isMtag()) {
                  grp.removeMember(this.connTable.getDeviceIndex(outDev));
                  tagCnctn.setAddressGroup(-1);
               }

               if (ins.length == 1 && !ins[0].isMtag()) {
                  BDeviceData dd = ins[0].getLonDevice().getDeviceData();
                  BIAddressEntry addrEntry = BAddressEntry.makeSubnetNodeEntry(
                     dd.getSubnetNodeId(), 2, dd.getWorkingDomain(), this.netMgmt.getLinkDescriptors().getDescriptor(2)
                  );
                  if (this.debug) {
                     System.out.println("\n  update message tagCnctn " + out.getTagIndex() + " in " + outDev.getDisplayName(null));
                  }

                  outDev.getDeviceData().setAddressEntry(out.getTagIndex(), addrEntry);

                  try {
                     this.updateAddressTable(outDev, out.getTagIndex());
                  } catch (Throwable var11) {
                     out.setStatus(BLonLinkStatus.comError);
                     return;
                  }

                  out.setStatus(BLonLinkStatus.bound);
                  this.userUpdate();
               } else if (!this.groupTable.selectTagGroup(tagCnctn, this.offDevs)) {
                  tagCnctn.setStatus(BLonLinkStatus.groupError);
               }
            }
         }
      }
   }

   private void selectGroup() {
      for (int i = 0; i < this.table.length; i++) {
         for (Connection cnctn = this.table[i]; cnctn != null; cnctn = cnctn.getSecondary()) {
            if (cnctn.isActive() && !cnctn.isError() && !cnctn.isPollOnly()) {
               if (cnctn.getHub().isProxy()) {
                  cnctn.setAddressChange(false);
               } else if (cnctn.getHub().isPseudo()) {
                  cnctn.clearAddressEntry();
               } else {
                  if (this.debug) {
                     System.out.println("\nselect addressing for connection " + cnctn.getSelector());
                  }

                  if (cnctn.isTurnAroundAddressEntry()) {
                     if (!this.groupTable.selectTurnAround(cnctn, this.offDevs)) {
                        cnctn.setStatus(BLonLinkStatus.groupError);
                     }
                  } else if (cnctn.getHub().isPolled()) {
                     LonPoint[] tgts = cnctn.getTargets();

                     for (int n = 0; n < tgts.length; n++) {
                        if (!tgts[n].isProxy() && tgts[n].isActive()) {
                           this.groupTable.selectSingle(cnctn, tgts[n], this.offDevs);
                        }
                     }
                  } else {
                     LonPoint snTgt = cnctn.getSubnetNodeAddressTarget();
                     if ((snTgt == null || !this.groupTable.selectSingle(cnctn, snTgt, this.offDevs)) && !this.groupTable.selectGroup(cnctn, this.offDevs)) {
                        cnctn.setStatus(BLonLinkStatus.groupError);
                     }
                  }
               }
            }
         }
      }
   }

   private boolean processSnAddress(Connection connection) {
      LonPoint adrPnt = connection.getPntRequiringAddressEntry();
      if (adrPnt != null && !adrPnt.isLocal() && !adrPnt.isPseudo()) {
         try {
            BLonDevice dev = adrPnt.getLonDevice();
            this.updateAddressTable(dev, adrPnt.getAddressIndex());
         } catch (Throwable var4) {
            adrPnt.setStatus(BLonLinkStatus.comError);
            this.error("Failed to update address in BLonBindJob.processSnAddress() " + adrPnt, var4);
            return false;
         }

         this.userUpdate();
         return true;
      } else {
         return true;
      }
   }

   private void updateAddressTable(BLonDevice dev, int index) throws Exception {
      if (Lon.n()) {
         NmUtil.setOfflineInBind(dev, this.offDevs);
         NmUtil.updateAddressTable(dev, index);
         if (!NmUtil.verifyAddressEntry(dev, index)) {
            this.error("ERROR: unable to verify address entry " + index + " in " + dev.getDisplayName(null), null);
         }
      }
   }

   private void processConnections() {
      for (int i = 0; i < this.table.length; i++) {
         for (Connection cnctn = this.table[i]; cnctn != null; cnctn = cnctn.getSecondary()) {
            if (!cnctn.isError() && !cnctn.isPollOnly() && (cnctn.getAddressGroup() >= 0 || !cnctn.isAddressChange() || this.processSnAddress(cnctn))) {
               if (cnctn.isLocal()) {
                  this.processLocalConnection(cnctn);
               } else {
                  this.processNetConnection(cnctn);
               }
            }
         }
      }
   }

   public void userUpdate() {
      if (++this.completes % 50 == 0) {
         this.incrementProgress(1, 80);
      }
   }

   private void removeObsolete() {
      for (int i = 0; i < this.table.length; i++) {
         for (Connection c = this.table[i]; c != null; c = c.getSecondary()) {
            boolean isPollOnly = c.isPollOnly();
            LonPoint hub = c.getHub();
            if (hub != null && hub.requiresUnbind(isPollOnly)) {
               this.unbind(hub);
            }

            LonPoint[] targets = c.getTargets();

            for (int x = 0; x < targets.length; x++) {
               if (targets[x].requiresUnbind(isPollOnly)) {
                  this.unbind(targets[x]);
               }
            }
         }
      }
   }

   private void processLocalConnection(Connection c) {
      int selector = c.getSelector();
      LonPoint hub = c.getHub();
      LonPoint[] targets = c.getTargets();

      for (int i = 0; i < targets.length; i++) {
         this.bind(targets[i], selector, c, false);
      }

      if (hub != null) {
         this.bind(hub, selector, c, !hub.isLocal());
      }
   }

   private void processNetConnection(Connection c) {
      int selector = c.getSelector();
      LonPoint hub = c.getHub();
      LonPoint[] targets = c.getTargets();

      for (int i = 0; i < targets.length; i++) {
         this.bind(targets[i], selector, c, false);
      }

      if (hub != null) {
         this.bind(hub, selector, c, false);
      }
   }

   private void bind(LonPoint lp, int selector, Connection conn, boolean localBind) {
      if (lp != null && !lp.isObsolete() && !lp.isUnbound() && !lp.isError()) {
         if (lp.isAliasPoint()) {
            this.bindAlias((LonPointAlias)lp, selector, conn);
         } else if (lp.isPseudo()) {
            this.bindPseudo((LonPointPseudo)lp, selector, conn);
         } else if (lp.isLocal()) {
            lp.setStatus(BLonLinkStatus.bound);
         } else {
            BNetworkVariable nv = lp.getNetworkVariable();
            BNvProps nvProps = nv.getNvProps();
            if (lp.isBound()) {
               if (nvProps.getBoundToLocal() != localBind) {
                  nvProps.setBoundToLocal(localBind);
                  nv.bound();
               }
            } else {
               BNvConfigData configData = nv.getNvConfigData();
               BNvConfigData origDat = (BNvConfigData)configData.newCopy(true);
               configData.setSelector(selector);
               configData.setAddrIndex(lp.getAddressIndex());
               configData.setAuthenticated(conn.isAuthenticated());
               configData.setServiceType(NmUtil.linkTypeToServiceType(conn.getLinkType()));
               configData.setPriority(conn.getPriority() && !configData.isInput());
               configData.setTurnAround(lp.isOutput() && conn.hasTurnAround());
               if (this.debug) {
                  System.out.println("\nin BLonBindJob.bind() for LonPoint " + lp + "\nnvprops =>" + nv.getNvProps());
               }

               BLonDevice dev = lp.getLonDevice();

               try {
                  if (Lon.n()) {
                     NmUtil.setOfflineInBind(dev, this.offDevs);
                     NmUtil.updateNvConfig(dev, nv.getNvIndex(), configData);
                     if (!NmUtil.verifyNvConfig(dev, nv.getNvIndex(), configData)) {
                        this.error("Failed to verifyNvConfig in BLonBindJob.bind() " + lp, null);
                        configData.copyFrom(origDat);
                        lp.setStatus(BLonLinkStatus.comError);
                        return;
                     }
                  }
               } catch (Throwable var12) {
                  this.error("Failed to update NV config in BLonBindJob.bind() " + lp, var12);
                  configData.copyFrom(origDat);
                  lp.setStatus(BLonLinkStatus.comError);
                  return;
               }

               lp.setStatus(BLonLinkStatus.bound);
               nv.getNvProps().setBoundToLocal(localBind);
               if (!localBind && !configData.isInput() && nv.getData().hasProxies()) {
                  System.out.println("INTERNAL ERROR : not local but has proxies for " + lp + "\n" + conn);
               }

               try {
                  nv.bound();
               } catch (Throwable var11) {
               }

               this.userUpdate();
            }
         }
      }
   }

   private void bindPseudo(LonPointPseudo lp, int selector, Connection conn) {
      BNetworkVariable nv = lp.pseudoNv;
      if (!lp.isBound()) {
         BNvConfigData configData = nv.getNvConfigData();
         configData.setSelector(selector);
         configData.setAddrIndex(lp.getAddressIndex());
         configData.setAuthenticated(conn.isAuthenticated());
         configData.setServiceType(NmUtil.linkTypeToServiceType(conn.getLinkType()));
         configData.setPriority(conn.getPriority() && !configData.isInput());
         configData.setTurnAround(lp.isOutput() && conn.hasTurnAround());
         if (this.debug) {
            System.out.println("\nin BLonBindJob.bind() for LonPoint " + lp + "\nnvprops =>" + nv.getNvProps());
         }

         lp.setStatus(BLonLinkStatus.bound);
         this.userUpdate();
      }
   }

   private void bindAlias(LonPointAlias lap, int selector, Connection conn) {
      if (lap != null && lap.isNew()) {
         BLonDevice dev = lap.getLonDevice();
         BAliasConfigData configData = dev.getDeviceData().getAliasTable().getAliasArray()[lap.getAliasIndex()];
         BAliasConfigData origDat = (BAliasConfigData)configData.newCopy(true);
         configData.setSelector(selector);
         configData.setAddrIndex(lap.getAddressIndex());
         configData.setAuthenticated(conn.isAuthenticated());
         configData.setServiceType(NmUtil.linkTypeToServiceType(conn.getLinkType()));
         configData.setPriority(conn.getPriority() && !configData.isInput());
         configData.setDirection(lap.getDirection());
         configData.setTurnAround(lap.isOutput() && conn.hasTurnAround());
         configData.setPrimary(lap.getPrimaryNvIndex());
         configData.setBound();
         if (this.debug) {
            System.out.println("\nin BLonBindJob.bind() for bindAlias " + lap + "\n  prim nvprops =>" + lap.getNvProps());
         }

         try {
            if (Lon.n()) {
               NmUtil.setOfflineInBind(dev, this.offDevs);
               NmUtil.updateAliasConfig(dev, lap.getAliasIndex(), configData);
               if (!NmUtil.verifyAliasNvConfig(dev, lap.getAliasIndex(), configData)) {
                  lap.setStatus(BLonLinkStatus.comError);
                  return;
               }
            }
         } catch (Throwable var8) {
            this.error("Failed to update NV config in BLonBindJob.bindAlias() " + lap, var8);
            configData.copyFrom(origDat);
            lap.setStatus(BLonLinkStatus.comError);
            return;
         }

         lap.setStatus(BLonLinkStatus.bound);
         this.userUpdate();
      }
   }

   private void unbind(LonPoint lp) {
      if (lp.isAliasPoint()) {
         this.unbindAlias((LonPointAlias)lp);
      } else {
         if (this.debug) {
            System.out.print("\nunbind() for " + lp);
         }

         BNetworkVariable nv = lp.getNetworkVariable();
         BNvConfigData nvCfg = (BNvConfigData)lp.getNvConfigData().newCopy(true);
         nvCfg.setUnbound(nv.getNvIndex());
         BLonDevice dev = lp.getLonDevice();

         try {
            if (Lon.n()) {
               NmUtil.setOfflineInBind(dev, this.offDevs);
               NmUtil.updateNvConfig(dev, nv.getNvIndex(), nvCfg);
               if (!NmUtil.verifyNvConfig(dev, nv.getNvIndex(), nvCfg)) {
                  lp.setStatus(BLonLinkStatus.comError);
                  return;
               }
            }
         } catch (Throwable var6) {
            this.error("Failed to update NV config in BLonBindJob.unbind()" + lp, var6);
            lp.setStatus(BLonLinkStatus.comError);
            return;
         }

         nv.setUnbound();
         lp.setStatus(BLonLinkStatus.unbound);
         this.userUpdate();
      }
   }

   private void unbindAlias(LonPointAlias lap) {
      if (this.debug) {
         System.out.print("\nunbindAlias() for  " + lap);
      }

      BLonDevice dev = lap.getLonDevice();
      BAliasTable aliasTable = dev.getDeviceData().getAliasTable();
      BAliasConfigData configData = aliasTable.getAliasEntry(lap.getAliasIndex());
      BAliasConfigData origDat = (BAliasConfigData)configData.newCopy(true);
      configData.clearData();

      try {
         if (Lon.n()) {
            NmUtil.setOfflineInBind(dev, this.offDevs);
            NmUtil.updateAliasConfig(dev, lap.getAliasIndex(), configData);
            if (!NmUtil.verifyAliasNvConfig(dev, lap.getAliasIndex(), configData)) {
               lap.setStatus(BLonLinkStatus.comError);
               return;
            }
         }
      } catch (Throwable var7) {
         this.error("Failed to update NV config in BLonBindJob.unbindAlias()" + lap, var7);
         configData.copyFrom(origDat);
         lap.setStatus(BLonLinkStatus.comError);
         return;
      }

      lap.setStatus(BLonLinkStatus.unbound);
      this.userUpdate();
   }

   private void verifyChannelPriorities() {
      BLonDevice[] lonDevices = this.adrMan.getDeviceList(false);
      Vector<BLonDevice> needPriority = new Vector<>();
      Vector<BLonDevice> delPriority = new Vector<>();

      for (int i = 0; i < lonDevices.length; i++) {
         BLonDevice dev = lonDevices[i];
         boolean needsPrioritySlot = false;
         BINetworkVariable[] nvs = dev.getNetworkVariables();

         for (int n = 0; n < nvs.length; n++) {
            if (nvs[n] != null && nvs[n].isNetworkVariable() && ((BNetworkVariable)nvs[n]).requiresPrioritySlot()) {
               needsPrioritySlot = true;
               break;
            }
         }

         if (needsPrioritySlot) {
            needPriority.addElement(dev);
            lonDevices[i] = null;
         }

         if (needsPrioritySlot != (dev.getDeviceData().getPrioritySlot() != 0) && !needsPrioritySlot) {
            delPriority.addElement(dev);
            lonDevices[i] = null;
         }
      }

      try {
         int numPriorities = needPriority.size();
         int numDeletes = delPriority.size();

         for (int i = 0; i < numPriorities; i++) {
            BLonDevice dev = needPriority.elementAt(i);
            dev.getDeviceData().setPrioritySlot(i + 1);
            Neuron.setNodePriorityInfo(dev, i + 1, numPriorities);
         }

         for (int i = 0; i < numDeletes; i++) {
            BLonDevice dev = delPriority.elementAt(i);
            dev.getDeviceData().setPrioritySlot(0);
            Neuron.setNodePriorityInfo(dev, 0, numPriorities);
         }

         if (numPriorities != this.netMgmt.getChannelPriorities()) {
            this.netMgmt.setChannelPriorities(numPriorities);

            for (int i = 0; i < lonDevices.length; i++) {
               BLonDevice dev = lonDevices[i];
               if (dev != null && !dev.getDeviceData().getFreezeChannelPriorities()) {
                  Neuron.setNodePriorityInfo(dev, 0, numPriorities);
               }
            }
         }
      } catch (LonException var9) {
         System.out.println("\n com failure in BLonBindJob.verifyChannelPriorities()" + var9);
      }
   }
}
