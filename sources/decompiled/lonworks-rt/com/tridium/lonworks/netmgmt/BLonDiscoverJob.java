package com.tridium.lonworks.netmgmt;

import com.tridium.lonworks.datatypes.BDiscoverParameter;
import com.tridium.lonworks.enums.BDiscoverMode;
import com.tridium.lonworks.enums.BLonRouterMode;
import com.tridium.lonworks.enums.BLonRouterType;
import com.tridium.lonworks.netmessages.QueryDomainResponse;
import com.tridium.lonworks.netmessages.QueryIdRequest;
import com.tridium.lonworks.netmessages.QueryIdResponse;
import com.tridium.lonworks.netmessages.RouterStatusResponse;
import com.tridium.lonworks.util.LonByteArrayUtil;
import com.tridium.lonworks.util.Neuron;
import com.tridium.lonworks.util.NmUtil;
import com.tridium.lonworks.util.RouterUtil;
import java.util.Vector;
import javax.baja.job.JobCancelException;
import javax.baja.lonworks.BLonNetwork;
import javax.baja.lonworks.FailedResponseException;
import javax.baja.lonworks.LonComm;
import javax.baja.lonworks.LonException;
import javax.baja.lonworks.datatypes.BDomainId;
import javax.baja.lonworks.datatypes.BNeuronId;
import javax.baja.lonworks.datatypes.BProgramId;
import javax.baja.lonworks.datatypes.BSubnetNode;
import javax.baja.lonworks.datatypes.LonAddress;
import javax.baja.lonworks.enums.BLonNodeState;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BIcon;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public class BLonDiscoverJob extends BLonNetmgmtJob implements NetMgmtConst {
   public static final Type TYPE = Sys.loadType(BLonDiscoverJob.class);
   private BDomainId ourDomain;
   Vector<BLonDiscoverJob.RouterData> newRtrs = new Vector<>();
   private static final BIcon icon = BIcon.std("find.png");
   private LonComm lonComm;
   private BDiscoverParameter param = null;
   Throwable abortReason = null;

   @Override
   public Type getType() {
      return TYPE;
   }

   public BLonDiscoverJob() {
   }

   public BLonDiscoverJob(BLonNetmgmt netMgmt) {
      super(netMgmt);
      this.lonComm = this.lon.lonComm();
   }

   public BLonDiscoverJob(BLonNetmgmt netMgmt, BDiscoverParameter p) {
      super(netMgmt);
      this.lonComm = this.lon.lonComm();
      this.param = p;
   }

   @Override
   public void run() {
      this.log().start("Discover devices ");
      this.doDiscover();
   }

   public BLonNetmgmt getNetmgmt() {
      return this.netMgmt;
   }

   public void doDiscover() {
      boolean tmpBrdg = this.netMgmt.getTempBridge();
      this.ourDomain = this.netMgmt.getDomainId();
      this.netMgmt.getDeviceDiscoverTable().clearEntries();
      this.netMgmt.getRouterDiscoverTable().clearEntries();

      try {
         if (!this.noTempBridge() && !tmpBrdg) {
            RouterUtil.setTemporaryBridge(this.lon);
         }

         NmUtil.initDiscover(this.lon, this.netMgmt, true);
         this.progress(5);
         QueryIdRequest rqst = this.getQueryRequest();
         int devCnt = 0;
         int wrkDmn = this.lon.getLocalLonDevice().getDeviceData().getWorkingDomain();
         int othDmn = wrkDmn == 0 ? 1 : 0;

         QueryIdResponse resp;
         while ((resp = NmUtil.queryId(this.lonComm, wrkDmn, rqst)) != null) {
            this.processResp(resp);
            if (++devCnt % 5 == 0) {
               this.incrementProgress(5, 75);
            }
         }

         this.progress(75);
         devCnt = 0;
         if (!this.netMgmt.getDomainId().isZeroLength()) {
            while ((resp = NmUtil.queryId(this.lonComm, othDmn, rqst)) != null) {
               this.processResp(resp);
               if (++devCnt % 5 == 0) {
                  this.incrementProgress(5, 95);
               }
            }
         }

         this.progress(95);
         this.netMgmt.fireDeviceDiscoveryUpdated(this.netMgmt.getDeviceDiscoverTable());
         this.netMgmt.fireRouterDiscoveryUpdated(this.netMgmt.getRouterDiscoverTable());
         this.success();
      } catch (JobCancelException var11) {
         this.canceled();
      } catch (Throwable var12) {
         this.fatal("Unexpected exception in BLonDiscoverJob.", var12);
      } finally {
         if (!this.noTempBridge()) {
            if (!tmpBrdg) {
               RouterUtil.clearTemporaryBridge(this.lon);
            }

            this.clearRouterTempBridge();
         }
      }
   }

   private QueryIdRequest getQueryRequest() {
      QueryIdRequest rqst = new QueryIdRequest(1);
      if (this.param == null) {
         return rqst;
      } else {
         byte[] data = null;

         try {
            data = LonByteArrayUtil.getBytes(this.param.getData());
         } catch (Throwable var4) {
         }

         BDiscoverMode mode = this.param.getMode();
         if (data != null && data.length > 0 && mode != BDiscoverMode.noOptions) {
            if (mode == BDiscoverMode.absolute) {
               rqst.setMode(0);
            }

            if (mode == BDiscoverMode.readOnlyRelative) {
               rqst.setMode(1);
            }

            if (mode == BDiscoverMode.configRelative) {
               rqst.setMode(2);
            }

            rqst.setOffset(this.param.getOffset());
            rqst.setData(data);
         }

         return rqst;
      }
   }

   private void processResp(QueryIdResponse resp) {
      boolean auth = this.netMgmt.getAuthenticate();

      try {
         this.netMgmt.log().fine("processResp " + resp);
         this.log().message("processResp " + resp);
         BNeuronId nid = resp.getNeuronId();
         NmUtil.setRespondToQuery(this.lonComm, nid, 0);
         BLonNetwork lon = this.netMgmt.lonNetwork();
         if (lon.findDevice(nid) != null || lon.findRouter(nid) != null) {
            NmUtil.wait(200);
            return;
         }

         int subnet = 0;
         int nodeId = 0;
         int wrkDmn = 0;

         try {
            QueryDomainResponse domain;
            try {
               domain = NmUtil.queryDomain(this.lonComm, nid, 0, auth);
            } catch (FailedResponseException var15) {
               domain = NmUtil.queryDomain(this.lonComm, nid, 0, true);
               auth = true;
            }

            if (!domain.sameDomain(this.ourDomain)) {
               try {
                  QueryDomainResponse domain1 = NmUtil.queryDomain(this.lonComm, nid, 1, auth);
                  if (domain1.sameDomain(this.ourDomain)) {
                     domain = domain1;
                     wrkDmn = 1;
                  }
               } catch (Throwable var14) {
               }
            }

            if (domain.inUse()) {
               subnet = domain.getSubnet();
               nodeId = domain.getNodeId();
            }
         } catch (Exception var16) {
         }

         BProgramId pId = resp.getIdString();
         BLonNodeState state = NmUtil.getDeviceState(this.lonComm, nid, false);
         if (pId.isRouter()) {
            QueryDomainResponse farDomain = NmUtil.queryDomain(this.lonComm, nid, 0, auth, true);
            RouterStatusResponse rtrStatus = RouterUtil.getRouterStatus(this.lonComm, nid, auth, false);
            BLonRouterType type = BLonRouterType.make(rtrStatus.getType());
            BLonRouterMode mode = BLonRouterMode.make(rtrStatus.getMode());
            this.netMgmt
               .getRouterDiscoverTable()
               .addEntry(
                  "",
                  type,
                  mode,
                  state,
                  Neuron.getChannelId(this.lonComm, nid, auth, false),
                  BSubnetNode.make(subnet, nodeId),
                  Neuron.getChannelId(this.lonComm, nid, auth, true),
                  BSubnetNode.make(farDomain.getSubnet(), farDomain.getNodeId()),
                  nid,
                  Neuron.getNeuronId(this.lonComm, nid, auth, true)
               );
            if ((type == BLonRouterType.configured || type == BLonRouterType.learning) && mode != BLonRouterMode.temporaryBridge && !this.noTempBridge()) {
               this.newRtrs.addElement(new BLonDiscoverJob.RouterData(nid, mode, auth));
               RouterUtil.setRouterMode(this.lonComm, nid, BLonRouterMode.temporaryBridge, auth);
            }
         } else {
            this.netMgmt
               .getDeviceDiscoverTable()
               .addEntry("", state, subnet, nodeId, nid, pId, Neuron.getChannelId(this.lonComm, nid, auth, false), auth, wrkDmn);
         }

         NmUtil.wait(200);
      } catch (LonException var17) {
         this.warning("Error: " + var17.getMessage());
      }
   }

   private void clearRouterTempBridge() {
      for (int i = 0; i < this.newRtrs.size(); i++) {
         BLonDiscoverJob.RouterData rd = this.newRtrs.elementAt(i);

         try {
            RouterUtil.setRouterMode(this.lonComm, rd.addr, rd.mode, rd.auth);
         } catch (Throwable var4) {
         }
      }
   }

   private boolean noTempBridge() {
      return this.param == null ? false : this.param.getManagedNetwork();
   }

   public BIcon getIcon() {
      return icon;
   }

   static class RouterData {
      LonAddress addr;
      BLonRouterMode mode;
      boolean auth;

      RouterData(LonAddress ad, BLonRouterMode m, boolean a) {
         this.addr = ad;
         this.mode = m;
         this.auth = a;
      }
   }
}
