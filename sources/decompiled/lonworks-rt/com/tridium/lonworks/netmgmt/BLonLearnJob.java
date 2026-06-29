package com.tridium.lonworks.netmgmt;

import com.tridium.lonworks.BLonRouter;
import com.tridium.lonworks.datatypes.BDeviceEntryTable;
import com.tridium.lonworks.datatypes.BLearnParameter;
import com.tridium.lonworks.device.BUploadJob;
import com.tridium.lonworks.device.DynaDev;
import com.tridium.lonworks.enums.BLonRouterMode;
import com.tridium.lonworks.enums.BLonRouterType;
import com.tridium.lonworks.netmessages.QueryDomainResponse;
import com.tridium.lonworks.netmessages.QueryIdResponse;
import com.tridium.lonworks.netmessages.RouterStatusResponse;
import com.tridium.lonworks.util.DeviceDef;
import com.tridium.lonworks.util.Neuron;
import com.tridium.lonworks.util.NmUtil;
import com.tridium.lonworks.util.RouterUtil;
import com.tridium.lonworks.util.selfdoc.DocToXDevice;
import com.tridium.lonworks.xml.XLonDevice;
import com.tridium.sys.transfer.TransferResult;
import com.tridium.sys.transfer.TransferStrategy;
import javax.baja.driver.loadable.BUploadParameters;
import javax.baja.job.JobCancelException;
import javax.baja.lonworks.AddressManager;
import javax.baja.lonworks.BDynamicDevice;
import javax.baja.lonworks.BLonDevice;
import javax.baja.lonworks.BLonNetwork;
import javax.baja.lonworks.LonComm;
import javax.baja.lonworks.LonException;
import javax.baja.lonworks.datatypes.BDeviceData;
import javax.baja.lonworks.datatypes.BDomainId;
import javax.baja.lonworks.datatypes.BImportParameters;
import javax.baja.lonworks.datatypes.BNeuronId;
import javax.baja.lonworks.datatypes.BProgramId;
import javax.baja.lonworks.datatypes.BSubnetNode;
import javax.baja.lonworks.enums.BLonNodeState;
import javax.baja.naming.BOrd;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.util.Array;
import javax.baja.nre.util.IntHashMap;
import javax.baja.space.Mark;
import javax.baja.sys.BBoolean;
import javax.baja.sys.BComplex;
import javax.baja.sys.BComponent;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public class BLonLearnJob extends BLonNetmgmtJob implements NetMgmtConst {
   public static final Type TYPE = Sys.loadType(BLonLearnJob.class);
   private BLonDevice[] origDevs;
   private BLonRouter[] origRtrs;
   private LonComm lonComm;
   private BDomainId ourDomain;
   private IntHashMap storage = new IntHashMap(127);
   private IntHashMap duplicateCheck = new IntHashMap(127);
   private boolean auth;
   private BLearnParameter command;
   private Array<BLonDevice> uploadDevs = new Array(BLonDevice.class);
   BComponent cont;

   @Override
   public Type getType() {
      return TYPE;
   }

   public BLonLearnJob() {
   }

   public BLonLearnJob(BLonNetmgmt netMgmt, BLearnParameter param) {
      super(netMgmt);
      this.command = param;
      this.lonComm = this.lon.lonComm();
      this.auth = netMgmt.getAuthenticate();
      this.ourDomain = netMgmt.getDomainId();
      this.origDevs = this.lon.addressManager().getDeviceList(false);
      this.origRtrs = this.lon.addressManager().getRouterList();
      this.cont = (BComponent)(param.getContainer().isNull() ? this.lon : (BComponent)param.getContainer().get());
   }

   @Override
   public void run() {
      try {
         BDeviceEntryTable det = this.netMgmt.getDeviceDiscoverTable();
         det.clearEntries();
         this.netMgmt.fireDeviceDiscoveryUpdated(det);
         this.doLearn();
      } catch (JobCancelException var2) {
         this.canceled();
      } catch (Throwable var3) {
         this.fatal("Learn failed ", var3);
      }

      this.netMgmt.fireLearnComplete(null);
      this.end();
   }

   private void doLearn() {
      Array<BLonLearnJob.LonDeviceIds> deviceIds = new Array(BLonLearnJob.LonDeviceIds.class);
      this.log().start("Learning network");
      boolean unmanaged = this.command.getUnmanagedNetwork();
      boolean tmpBrdg = this.netMgmt.getTempBridge();

      try {
         if (!tmpBrdg) {
            RouterUtil.setTemporaryBridge(this.lon);
         }

         NmUtil.initDiscover(this.lon, this.netMgmt, false);
         this.initDuplicateCheck();
         this.netMgmt.log().fine("Discovering devices ");
         int wrkDmn = this.lon.getLocalLonDevice().getDeviceData().getWorkingDomain();

         QueryIdResponse resp;
         while ((resp = NmUtil.queryId(this.lonComm, 1, wrkDmn)) != null) {
            String msg = "Received QueryId response " + resp.toString();
            this.netMgmt.log().fine(msg);
            this.log().message(msg);
            NmUtil.wait(200);
            BProgramId pId = resp.getIdString();
            BNeuronId nId = resp.getNeuronId();
            NmUtil.setRespondToQuery(this.lonComm, nId, 0);
            this.incrementProgress(2, 70);
            boolean authOn = Neuron.isNMAuthSet(this.lonComm, nId, this.auth, false);
            BLonNodeState state = NmUtil.getDeviceState(this.lonComm, nId, authOn);
            if (unmanaged || state == BLonNodeState.configOnline) {
               BLonLearnJob.LonDeviceIds devIds = new BLonLearnJob.LonDeviceIds(nId, pId, authOn, state);
               if (!this.processDomainAndSubnetNode(devIds, false)) {
                  return;
               }

               if (pId.isRouter()) {
                  BLonNodeState farState = NmUtil.getDeviceState(this.lonComm, nId, authOn, true);
                  devIds.farIds = new BLonLearnJob.LonDeviceIds(devIds.nId, devIds.pId, devIds.authOn, farState);
                  if (!this.processDomainAndSubnetNode(devIds.farIds, true)) {
                     return;
                  }

                  if (this.matchExistingRouter(devIds)) {
                     continue;
                  }

                  RouterStatusResponse rtrStatus = RouterUtil.getRouterStatus(this.lonComm, nId, this.auth, false);
                  RouterUtil.setRouterMode(this.lonComm, nId, BLonRouterMode.temporaryBridge, this.auth);
                  devIds.rtrStatus = rtrStatus;
               } else if (this.matchExisting(devIds)) {
                  continue;
               }

               deviceIds.add(devIds);
            }
         }

         this.progress(70);
         BLonLearnJob.LonDeviceIds[] ids = (BLonLearnJob.LonDeviceIds[])deviceIds.trim();
         this.netMgmt.log().fine("Seek matches for " + ids.length + " unmatched devices");

         for (int i = 0; i < ids.length; i++) {
            BLonLearnJob.LonDeviceIds devIdsx = ids[i];
            ids[i] = null;
            this.incrementProgress(2, 90);
            if (devIdsx.pId.isRouter()) {
               this.addNewRouter(devIdsx);
            } else if (!this.matchProgramId(devIdsx)) {
               if (devIdsx.duplicate) {
                  ids[i] = devIdsx;
               } else {
                  this.addDynamic(devIdsx);
               }
            }
         }

         for (int ix = 0; ix < ids.length; ix++) {
            if (ids[ix] != null) {
               this.addDynamic(ids[ix]);
            }
         }

         this.emptyStorage();
         if (this.command.getLearnLinks()) {
            BLonDevice[] devList = this.lon.addressManager().getDeviceList(false);
            if (devList.length > 0) {
               BLonLearnLinksJob learnLinks = new BLonLearnLinksJob(this.netMgmt, devList, true);
               learnLinks.doLearnLinks();
            }
         }

         if (this.command.getUploadConfigData()) {
            BLonDevice[] upDevs = (BLonDevice[])this.uploadDevs.trim();
            BUploadParameters up = new BUploadParameters();
            up.setUploadTransient(false);

            for (int ixx = 0; ixx < upDevs.length; ixx++) {
               this.log().message("upload " + upDevs[ixx].getDisplayName(null));
               BUploadJob upJob = new BUploadJob(upDevs[ixx], up, null);
               upJob.run();
            }
         }

         this.success();
      } catch (JobCancelException var18) {
         throw var18;
      } catch (Exception var19) {
         this.fatal("Learn request failed.", var19);
      } finally {
         if (!tmpBrdg) {
            RouterUtil.clearTemporaryBridge(this.lon);
         }
      }
   }

   private boolean processDomainAndSubnetNode(BLonLearnJob.LonDeviceIds devIds, boolean farSide) throws LonException {
      boolean authOn = devIds.authOn;
      BNeuronId nId = devIds.nId;
      BProgramId pId = devIds.pId;
      devIds.twoDomains = Neuron.isTwoDomains(this.lonComm, nId, authOn, farSide);
      QueryDomainResponse domain = NmUtil.queryDomain(this.lonComm, nId, 0, authOn, farSide);
      if (devIds.twoDomains && !domain.sameDomain(this.ourDomain)) {
         domain = NmUtil.queryDomain(this.lonComm, nId, 1, authOn, farSide);
         devIds.wrkDmn = 1;
      } else {
         devIds.wrkDmn = 0;
      }

      devIds.duplicate = this.detectDuplicate(domain, devIds.nId, farSide);
      if (!this.command.getUnmanagedNetwork() && devIds.duplicate) {
         this.fatal("Duplicate subnet node detected - " + Integer.toString(domain.getSubnet()) + "/" + Integer.toString(domain.getNodeId()));
         this.end();
         return false;
      } else {
         devIds.domain = domain;
         devIds.chanId = Neuron.getChannelId(this.lonComm, nId, authOn, farSide);
         return true;
      }
   }

   private boolean matchExisting(BLonLearnJob.LonDeviceIds devIds) throws LonException {
      QueryDomainResponse domain = devIds.domain;
      BLonDevice dev = this.lon.addressManager().getDeviceByAddress(BSubnetNode.make(domain.getSubnet(), domain.getNodeId()));
      if (dev == null) {
         return false;
      } else {
         BDeviceData dd = dev.getDeviceData();
         if (!dd.getNeuronId().isZero()) {
            return false;
         } else {
            BProgramId devPid = dd.getProgramId();
            if (!devPid.isZero() && !devPid.equals(devIds.pId)) {
               this.lon.addressManager().unregisterLonDevice(dev);
               this.storage.put(getDeviceHash(dev), dev);
               this.netMgmt.log().fine("found subnet node - programIds don't match move " + dev.getDisplayName(null) + " to storage.");
               return false;
            } else {
               this.netMgmt.log().fine("found match remove from device list for " + dev);
               this.setDeviceData(dd, devIds);
               this.uploadDevs.add(dev);
               return true;
            }
         }
      }
   }

   private boolean matchExistingRouter(BLonLearnJob.LonDeviceIds devIds) throws LonException {
      for (int i = 0; i < this.origRtrs.length; i++) {
         if (this.origRtrs[i] != null) {
            BLonRouter rtr = this.origRtrs[i];
            if (rtr.getNearDeviceData().getNeuronId().isZero()
               && rtr.getNearDeviceData().getChannelId() == devIds.chanId
               && rtr.getFarDeviceData().getChannelId() == devIds.farIds.chanId) {
               this.netMgmt.log().fine("found match for " + rtr.getDisplayName(null));
               this.setDeviceData(rtr.getNearDeviceData(), devIds);
               this.setDeviceData(rtr.getFarDeviceData(), devIds.farIds);
               RouterUtil.uploadTypeAndMode(rtr);
               return true;
            }
         }
      }

      return false;
   }

   private boolean matchProgramId(BLonLearnJob.LonDeviceIds devIds) {
      int avalDev = -1;

      for (int i = 0; i < this.origDevs.length; i++) {
         BLonDevice dev = this.origDevs[i];
         if (dev != null) {
            BDeviceData dd = dev.getDeviceData();
            if (dd.getNeuronId().isZero() && dd.getProgramId().equals(devIds.pId)) {
               this.netMgmt.log().fine("found matching device " + dev.getDisplayName(null));
               this.matchDevice(dev, devIds);
               this.origDevs[i] = null;
               return true;
            }

            if (avalDev == -1 && dd.getProgramId().isZero()) {
               avalDev = i;
            }
         }
      }

      if (avalDev >= 0) {
         this.netMgmt.log().fine(" matching node " + devIds.getNodeId() + " with dynamic device");
         BLonDevice aDev = this.origDevs[avalDev];
         this.origDevs[avalDev] = null;
         this.matchDevice(aDev, devIds);
         aDev.getDeviceData().setProgramId(devIds.pId);
         if (aDev.getType().is(BDynamicDevice.TYPE)) {
            this.importXmlFile((BDynamicDevice)aDev, devIds.pId);
            this.buildDevice((BDynamicDevice)aDev);
         }

         this.uploadDevs.add(aDev);
         return true;
      } else {
         this.netMgmt.log().fine("no programid match found for node " + devIds.getNodeId());
         return false;
      }
   }

   private void matchDevice(BLonDevice dev, BLonLearnJob.LonDeviceIds devIds) {
      if (this.storage.get(getDeviceHash(dev)) != null) {
         this.storage.remove(getDeviceHash(dev));
         this.netMgmt.log().fine(" * match device from storage for s/n " + devIds.getSubnet() + "/" + devIds.getNodeId() + "\n");
      } else {
         this.lon.addressManager().unregisterLonDevice(dev);
         this.netMgmt.log().fine(" * device not in storage  s/n " + devIds.getSubnet() + "/" + devIds.getNodeId() + "\n");
      }

      this.setDeviceData(dev.getDeviceData(), devIds);
      this.lon.addressManager().registerLonDevice(dev);
   }

   private void setDeviceData(BDeviceData dd, BLonLearnJob.LonDeviceIds devIds) {
      dd.set(BDeviceData.neuronId, devIds.nId, AddressManager.noDeviceChange);
      dd.set(BDeviceData.programId, devIds.pId, AddressManager.noDeviceChange);
      dd.setInt(BDeviceData.workingDomain, devIds.wrkDmn, AddressManager.noDeviceChange);
      dd.setBoolean(BDeviceData.authenticate, devIds.authOn, AddressManager.noDeviceChange);
      dd.setBoolean(BDeviceData.twoDomains, devIds.twoDomains, AddressManager.noDeviceChange);
      dd.setInt(BDeviceData.channelId, devIds.chanId, AddressManager.noDeviceChange);
      BLonNodeState ns = devIds.state;
      if (!devIds.duplicate) {
         dd.set(BDeviceData.subnetNodeId, BSubnetNode.make(devIds.getSubnet(), devIds.getNodeId()), AddressManager.noDeviceChange);
      } else if (ns == BLonNodeState.configOnline || ns == BLonNodeState.configOffline) {
         ns = BLonNodeState.unconfigured;
      }

      dd.set(BDeviceData.nodeState, ns, AddressManager.noDeviceChange);
   }

   private void emptyStorage() {
      BLonDevice[] devs = new BLonDevice[this.storage.size()];
      this.storage.toArray(devs);
      this.netMgmt.log().fine("EmptyStorage()");

      for (int i = 0; i < devs.length; i++) {
         BLonDevice dev = devs[i];
         this.netMgmt.log().fine("moving " + dev.getDisplayName(null) + " addr " + dev.getDeviceData().getSubnetNodeId());
         dev.getDeviceData().set(BDeviceData.subnetNodeId, BSubnetNode.DEFAULT, AddressManager.noDeviceChange);
         this.lon.addressManager().registerLonDevice(dev);
         this.netMgmt.log().fine("new addr " + dev.getDeviceData().getSubnetNodeId());
      }
   }

   private void initDuplicateCheck() {
      for (int i = 0; i < this.origDevs.length; i++) {
         BLonDevice dev = this.origDevs[i];
         if (dev != null && !dev.getDeviceData().getNeuronId().isZero() && !dev.isLocal()) {
            this.duplicateCheck.put(getDeviceHash(dev), dev);
         }
      }

      for (int ix = 0; ix < this.origRtrs.length; ix++) {
         BLonRouter rtr = this.origRtrs[ix];
         if (rtr != null && !rtr.getNearDeviceData().getNeuronId().isZero()) {
            this.duplicateCheck.put(getDeviceHash(rtr.getNearDeviceData()), rtr);
            this.duplicateCheck.put(getDeviceHash(rtr.getFarDeviceData()), rtr);
         }
      }
   }

   private boolean detectDuplicate(QueryDomainResponse domain, BNeuronId nId, boolean farSide) {
      int hashKey = getDeviceHash(domain.getSubnet(), domain.getNodeId());
      if (this.duplicateCheck.get(hashKey) != null) {
         BLonDevice dev = this.lon.addressManager().getDeviceByAddress(BSubnetNode.make(domain.getSubnet(), domain.getNodeId()));
         return dev == null || !dev.getDeviceData().getNeuronId().equals(nId);
      } else {
         this.duplicateCheck.put(hashKey, domain);
         return false;
      }
   }

   private void addNewRouter(BLonLearnJob.LonDeviceIds devIds) throws Exception {
      BNeuronId destAddr = devIds.nId;
      this.netMgmt.log().fine("Add router between channels " + devIds.chanId + " & " + devIds.farIds.chanId + ".");
      BLonRouter rtr = new BLonRouter();
      this.setDeviceData(rtr.getNearDeviceData(), devIds);
      this.setDeviceData(rtr.getFarDeviceData(), devIds.farIds);
      rtr = (BLonRouter)this.addDevice(rtr, "LonRouter");
      rtr.set(BLonRouter.routerType, BLonRouterType.make(devIds.rtrStatus.getType()), BLonNetwork.lonNoWrite);
      rtr.set(BLonRouter.routerMode, BLonRouterMode.make(devIds.rtrStatus.getMode()), BLonNetwork.lonNoWrite);
      RouterUtil.uploadRouterTables(rtr);
   }

   private boolean addDynamic(BLonLearnJob.LonDeviceIds devIds) throws Exception {
      DeviceDef def = new DeviceDef(devIds.pId);
      BLonDevice dev = this.createDeviceForPid(def);
      String devName = def.getName();
      if (devName == null) {
         devName = "LonDevice";
      }

      this.setDeviceData(dev.getDeviceData(), devIds);
      dev = (BLonDevice)this.addDevice(dev, devName);
      if (dev.getType().is(BDynamicDevice.TYPE)) {
         this.buildDevice((BDynamicDevice)dev);
      }

      this.uploadDevs.add(dev);
      return true;
   }

   private BComplex addDevice(BComplex dev, String devName) throws Exception {
      String n = devName;
      int cnt = 1;

      while (this.cont.get(n) != null) {
         n = devName + "_" + cnt++;
      }

      BComponent params = new BComponent();
      params.add("exact", BBoolean.TRUE);
      Mark mark = new Mark(dev, n);
      TransferResult r = TransferStrategy.make(16, mark, this.cont, params, null).transfer();
      String[] a = r.getInsertNames();
      NmUtil.wait(500);
      return (BComplex)this.cont.get(a[0]);
   }

   private boolean importXmlFile(BDynamicDevice dev, BProgramId pId) {
      BOrd ord = null;
      DeviceDef def = new DeviceDef(pId);
      if (def.isXml()) {
         ord = def.getXmlOrd();
      }

      if (ord == null) {
         return false;
      } else {
         dev.setXmlFile(ord);
         return true;
      }
   }

   private BLonDevice createDeviceForPid(DeviceDef def) {
      BOrd ord = null;
      if (def.isClass()) {
         return def.getDevice();
      } else {
         if (def.isXml()) {
            ord = def.getXmlOrd();
         }

         BDynamicDevice dev = new BDynamicDevice();
         if (ord != null) {
            dev.setXmlFile(ord);
         }

         return dev;
      }
   }

   private void buildDevice(BDynamicDevice dev) {
      if (dev.getXmlFile() != BOrd.NULL) {
         try {
            dev.doImportXml(new BImportParameters(false, this.netMgmt.getUseLonObjects()));
            this.status("Import file for " + dev.getDisplayName(null));
            return;
         } catch (Throwable var4) {
            this.warning(dev.getDisplayName(null) + " failed to import " + dev.getXmlFile().toString(), var4);
         }
      }

      try {
         if (dev.getDeviceData().getNodeState() == BLonNodeState.applicationless) {
            return;
         }

         this.status("Import selfdoc for " + dev.getDisplayName(null));
         XLonDevice xdev = DocToXDevice.extract(dev.getNeuronIdAddress(), this.lonComm, this.auth);
         DynaDev.importXLon(dev, xdev, this.netMgmt.getUseLonObjects());
      } catch (Throwable var3) {
         this.warning("Failed to learn nvs for " + dev.getDisplayName(null), var3);
      }
   }

   private static int getDeviceHash(BLonDevice dev) {
      return getDeviceHash(dev.getDeviceData());
   }

   private static int getDeviceHash(BDeviceData dd) {
      BSubnetNode sn = dd.getSubnetNodeId();
      return getDeviceHash(sn.getSubnetId(), sn.getNodeId());
   }

   private static int getDeviceHash(int subnet, int node) {
      return (subnet << 7) + node;
   }

   private class LonDeviceIds {
      public QueryDomainResponse domain;
      public int wrkDmn;
      public boolean authOn;
      public BNeuronId nId;
      public BProgramId pId;
      public boolean twoDomains;
      public boolean duplicate;
      public int chanId;
      public BLonNodeState state;
      BLonLearnJob.LonDeviceIds farIds;
      RouterStatusResponse rtrStatus = null;

      LonDeviceIds(BNeuronId nId, BProgramId pId, boolean authOn, BLonNodeState state) {
         this.nId = nId;
         this.pId = pId;
         this.authOn = authOn;
         this.state = state;
      }

      private int getSubnet() {
         return this.domain.getSubnet();
      }

      private int getNodeId() {
         return this.domain.getNodeId();
      }
   }
}
