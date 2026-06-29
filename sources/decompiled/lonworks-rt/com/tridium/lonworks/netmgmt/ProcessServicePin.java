package com.tridium.lonworks.netmgmt;

import com.tridium.lonworks.enums.BLonRouterMode;
import com.tridium.lonworks.enums.BLonRouterType;
import com.tridium.lonworks.netmessages.QueryDomainResponse;
import com.tridium.lonworks.netmessages.RouterStatusResponse;
import com.tridium.lonworks.netmessages.ServicePin;
import com.tridium.lonworks.util.Neuron;
import com.tridium.lonworks.util.NmUtil;
import com.tridium.lonworks.util.RouterUtil;
import javax.baja.lonworks.BLonNetwork;
import javax.baja.lonworks.LonComm;
import javax.baja.lonworks.datatypes.BDomainId;
import javax.baja.lonworks.datatypes.BNeuronId;
import javax.baja.lonworks.datatypes.BProgramId;
import javax.baja.lonworks.datatypes.BSubnetNode;
import javax.baja.lonworks.enums.BLonNodeState;

public class ProcessServicePin implements Runnable, NetMgmtConst {
   private BLonNetwork lon;
   private BLonNetmgmt netmgmt;
   private LonComm lonComm;
   private ServicePin msg;

   public ProcessServicePin(BLonNetmgmt netmgmt, ServicePin msg) {
      this.netmgmt = netmgmt;
      this.lon = netmgmt.lonNetwork();
      this.msg = msg;
      this.lonComm = this.lon.lonComm();
   }

   @Override
   public void run() {
      BNeuronId nid = this.msg.getNeuronId();
      BProgramId pid = this.msg.getIdString();
      if (this.netmgmt.getDeviceDiscoverTable().findEntry(nid) == null
         && this.netmgmt.getRouterDiscoverTable().findEntry(nid) == null
         && this.lon.findDevice(nid) == null
         && this.lon.findRouter(nid) == null) {
         int subnet = 0;
         int nodeId = 0;
         int wrkDmn = 0;

         try {
            BDomainId ourDomain = this.netmgmt.getDomainId();
            QueryDomainResponse domain = NmUtil.queryDomain(this.lonComm, nid, 0, this.netmgmt.getAuthenticate());
            if (!domain.sameDomain(ourDomain)) {
               try {
                  QueryDomainResponse domain1 = NmUtil.queryDomain(this.lonComm, nid, 1, this.netmgmt.getAuthenticate());
                  if (domain1.sameDomain(ourDomain)) {
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

            BLonNodeState state = NmUtil.getDeviceState(this.lonComm, nid, false);
            boolean auth = this.netmgmt.getAuthenticate();
            if (pid.isRouter()) {
               QueryDomainResponse farDomain = NmUtil.queryDomain(this.lonComm, nid, 0, auth, true);
               RouterStatusResponse rtrStatus = RouterUtil.getRouterStatus(this.lonComm, nid, auth, false);
               BLonRouterType type = BLonRouterType.make(rtrStatus.getType());
               BLonRouterMode mode = BLonRouterMode.make(rtrStatus.getMode());
               this.netmgmt
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
               this.netmgmt.fireRouterDiscoveryUpdated(this.netmgmt.getRouterDiscoverTable());
            } else {
               this.netmgmt
                  .getDeviceDiscoverTable()
                  .addEntry(
                     "",
                     state,
                     subnet,
                     nodeId,
                     nid,
                     pid,
                     Neuron.getChannelId(this.lonComm, nid, auth, false),
                     Neuron.isNMAuthSet(this.lonComm, nid, auth, false),
                     wrkDmn
                  );
               this.netmgmt.fireDeviceDiscoveryUpdated(this.netmgmt.getDeviceDiscoverTable());
            }
         } catch (Exception var15) {
            System.out.println(var15);
         }
      }
   }
}
