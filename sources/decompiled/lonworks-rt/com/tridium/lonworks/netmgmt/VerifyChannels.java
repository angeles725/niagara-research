package com.tridium.lonworks.netmgmt;

import com.tridium.lonworks.BLonRouter;
import com.tridium.lonworks.netmessages.QueryIdResponse;
import com.tridium.lonworks.netmessages.SetNodeModeRequest;
import com.tridium.lonworks.util.Neuron;
import com.tridium.lonworks.util.NmUtil;
import com.tridium.lonworks.util.RouterUtil;
import java.io.PrintWriter;
import javax.baja.lonworks.BLonDevice;
import javax.baja.lonworks.BLonNetwork;
import javax.baja.lonworks.LonComm;
import javax.baja.lonworks.LonException;
import javax.baja.lonworks.datatypes.BNeuronId;
import javax.baja.lonworks.datatypes.LonAddress;
import javax.baja.lonworks.enums.BLonRepeatTimer;

public class VerifyChannels {
   private static final int ROUTER_INIT = 0;
   private static final int ROUTER_DISABLED = 1;
   private BLonNetwork lon;
   private BLonNetmgmt netmgmt;
   private LonComm lonComm;
   private VerifyChannels.Router[] rtrs;
   private BLonDevice[] devs;
   int currentChanId = -1;
   PrintWriter out;

   public static void verify(BLonNetwork lon, PrintWriter out) throws Exception {
      VerifyChannels vc = new VerifyChannels(lon);
      vc.out = out;
      vc.doVerify();
   }

   private VerifyChannels(BLonNetwork lon) {
      this.lon = lon;
      this.netmgmt = lon.netmgmt();
      this.lonComm = lon.lonComm();
      this.devs = lon.getLonDevices();

      for (int i = 0; i < this.devs.length; i++) {
         if (this.devs[i].isLocal()) {
            this.devs[i] = null;
            break;
         }
      }

      BLonRouter[] brs = lon.getLonRouters();
      this.rtrs = new VerifyChannels.Router[brs.length];

      for (int ix = 0; ix < brs.length; ix++) {
         this.rtrs[ix] = new VerifyChannels.Router(brs[ix]);
      }
   }

   private void doVerify() throws Exception {
      boolean tmpBrdg = this.netmgmt.getTempBridge();

      try {
         if (!tmpBrdg) {
            RouterUtil.setTemporaryBridge(this.lon);
         }

         this.enableRespondToQuery(this.lon);
         this.processChannel(this.lon.getLocalLonDevice().getDeviceData().getChannelId());
         this.processRemaining();
      } finally {
         if (!tmpBrdg) {
            RouterUtil.clearTemporaryBridge(this.lon);
         }
      }
   }

   private void processChannel(int chanId) throws LonException {
      this.out.println("\nverify channel " + chanId);
      this.disableRouters();
      this.disableRespondToQuery(chanId);
      this.identifyChannelNodes(this.lon, chanId);

      for (int i = 0; i < this.rtrs.length; i++) {
         if (this.rtrs[i] != null) {
            BLonRouter rtr = this.rtrs[i].rtr;
            if (rtr.getNearDeviceData().getChannelId() == chanId && this.enableRouter(rtr)) {
               this.rtrs[i] = null;
               this.processChannel(rtr.getFarDeviceData().getChannelId());
            }
         }
      }
   }

   private void disableRespondToQuery(int chanId) throws LonException {
      for (int i = 0; i < this.devs.length; i++) {
         BLonDevice d = this.devs[i];
         if (d != null && d.getDeviceData().getChannelId() == chanId) {
            try {
               NmUtil.setRespondToQuery(this.lonComm, NmUtil.getSendAddress(d), 0);
               this.devs[i] = null;
            } catch (LonException var6) {
            }
         }
      }

      for (int ix = 0; ix < this.rtrs.length; ix++) {
         if (this.rtrs[ix] != null) {
            BLonRouter rtr = this.rtrs[ix].rtr;
            if (rtr.getNearDeviceData().getChannelId() == chanId) {
               try {
                  NmUtil.setRespondToQuery(this.lonComm, NmUtil.getSendAddress(rtr), 0);
               } catch (Exception var5) {
               }
            }
         }
      }
   }

   private void identifyChannelNodes(BLonNetwork lon, int chanId) throws LonException {
      this.currentChanId = chanId;
      int wrkDmn = lon.getLocalLonDevice().getDeviceData().getWorkingDomain();

      QueryIdResponse resp;
      while ((resp = NmUtil.queryId(this.lonComm, 1, wrkDmn)) != null) {
         this.processResp(resp);
      }

      this.currentChanId = -1;
   }

   private void processResp(QueryIdResponse resp) throws LonException {
      BNeuronId nid = resp.getNeuronId();

      try {
         NmUtil.setRespondToQuery(this.lonComm, nid, 0);
      } catch (Exception var4) {
      }

      if (resp.getIdString().isRouter()) {
         this.scanForRouter(nid);
      } else {
         this.scanForDevice(nid);
      }
   }

   private void scanForDevice(BNeuronId nid) throws LonException {
      BLonDevice dev = null;

      for (int i = 0; i < this.devs.length; i++) {
         if (this.devs[i] != null && nid.equals(this.devs[i].getNeuronIdAddress())) {
            dev = this.devs[i];
            this.devs[i] = null;
         }
      }

      if (dev == null) {
         if ((dev = this.lon.findDevice(nid)) == null) {
            int myChanId = Neuron.getChannelId(this.lonComm, nid, false, false);
            this.out.print("   found new device (neuronId=" + nid + " on channel " + this.currentChanId);
            if (myChanId != this.currentChanId) {
               this.out.print(" - reports channelId=" + myChanId);
            }

            this.out.println("\n");
            return;
         }

         if (!dev.isDown()) {
            return;
         }
      }

      int myChanId = dev.getDeviceData().getChannelId();
      if (myChanId != this.currentChanId) {
         this.out
            .println(
               "   "
                  + dev.getDisplayName(null)
                  + " "
                  + dev.getSubnetNodeAddress()
                  + " found on channel "
                  + this.currentChanId
                  + " - db channelId="
                  + myChanId
                  + "\n"
            );
      }
   }

   private void scanForRouter(BNeuronId nid) throws LonException {
      BLonRouter rtr = null;

      for (int i = 0; i < this.rtrs.length; i++) {
         if (this.rtrs[i] != null && nid.equals(this.rtrs[i].rtr.getNeuronIdAddress())) {
            rtr = this.rtrs[i].rtr;
            this.rtrs[i] = null;
            break;
         }
      }

      if (rtr == null && (rtr = this.lon.findRouter(nid)) == null) {
         int myChanId = Neuron.getChannelId(this.lonComm, nid, false, false);
         this.out.print("   Found new router (neuronId=" + nid + ") on channel " + this.currentChanId);
         if (myChanId != this.currentChanId) {
            this.out.print(" - reports channelId=" + myChanId);
         }

         this.out.println("\n");
      } else {
         int myChanId = rtr.getNearDeviceData().getChannelId();
         if (myChanId != this.currentChanId) {
            this.out
               .println(
                  "   "
                     + rtr.getDisplayName(null)
                     + " "
                     + rtr.getNearDeviceData().getSubnetNodeId()
                     + " found on channel "
                     + this.currentChanId
                     + " - db channelId="
                     + myChanId
                     + "\n"
               );
         }
      }
   }

   private void processRemaining() {
      String brk = "\n\n*******\n";
      boolean found = false;

      for (int i = 0; i < this.devs.length; i++) {
         BLonDevice d = this.devs[i];
         if (d != null) {
            if (!found) {
               this.out.println(brk);
               found = true;
            }

            this.out.println(" Unable to communicate with " + d.getDisplayName(null) + " " + d.getSubnetNodeAddress());
         }
      }

      for (int ix = 0; ix < this.rtrs.length; ix++) {
         if (this.rtrs[ix] != null) {
            if (!found) {
               this.out.println(brk);
               found = true;
            }

            BLonRouter r = this.rtrs[ix].rtr;
            this.out.println(" Unable to communicate with " + r.getDisplayName(null) + " " + r.getNearDeviceData().getSubnetNodeId() + "\n");
         }
      }
   }

   private void enableRespondToQuery(BLonNetwork lon) throws LonException {
      int wrkDmn = lon.getLocalLonDevice().getDeviceData().getWorkingDomain();
      int othDmn = wrkDmn == 0 ? 1 : 0;
      NmUtil.setRespondToQuery(lon.lonComm(), 1, wrkDmn);
      if (this.netmgmt.getDomainId().getLength() != 0) {
         NmUtil.setRespondToQuery(lon.lonComm(), 1, othDmn);
      }

      NmUtil.wait(500);
   }

   private void disableRouters() throws LonException {
      for (int i = 0; i < this.rtrs.length; i++) {
         if (this.rtrs[i] != null) {
            try {
               if (this.rtrs[i].state != 1) {
                  this.disableRouter(this.rtrs[i].rtr);
               }

               this.rtrs[i].state = 1;
            } catch (LonException var3) {
            }
         }
      }
   }

   private void disableRouter(BLonRouter rtr) throws LonException {
      setNodeState(this.lonComm, NmUtil.getSendAddress(rtr), 3, false, false);
   }

   private boolean enableRouter(BLonRouter rtr) {
      try {
         NmUtil.setDeviceState(rtr, rtr.getNearDeviceData().getNodeState(), false);
         return true;
      } catch (LonException var3) {
         return false;
      }
   }

   private static void setNodeState(LonComm lonComm, LonAddress destAddr, int state, boolean auth, boolean farSide) throws LonException {
      SetNodeModeRequest setMode = new SetNodeModeRequest(3, state);
      if (farSide) {
         setMode.setFarSide();
      }

      setMode.setAuthenticate(auth);
      setMode.setTransmitTimer(BLonRepeatTimer.milliSec192);
      lonComm.sendAcked(destAddr, setMode);
   }

   private static class Router {
      BLonRouter rtr;
      int state = 0;

      Router(BLonRouter r) {
         this.rtr = r;
      }
   }
}
