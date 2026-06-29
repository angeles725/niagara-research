package com.tridium.lonworks;

import com.tridium.lonworks.local.SnvtInfo;
import com.tridium.lonworks.loncomm.NLonComm;
import com.tridium.lonworks.netmessages.FetchNvRequest;
import com.tridium.lonworks.netmessages.FetchNvResponse;
import com.tridium.lonworks.netmessages.NetMessages;
import com.tridium.lonworks.netmessages.NoDataResponse;
import com.tridium.lonworks.netmessages.QueryNvConfigRequest;
import com.tridium.lonworks.netmessages.QueryNvConfigResponse;
import com.tridium.lonworks.netmessages.QuerySNVTRequest;
import com.tridium.lonworks.netmessages.QuerySNVTResponse;
import com.tridium.lonworks.netmessages.ServicePin;
import com.tridium.lonworks.netmessages.SetNodeModeRequest;
import com.tridium.lonworks.netmessages.UpdateNvConfigRequest;
import com.tridium.lonworks.netmessages.WinkRequest;
import javax.baja.lonworks.BINetworkVariable;
import javax.baja.lonworks.BLonNetwork;
import javax.baja.lonworks.LonListener;
import javax.baja.lonworks.LonMessage;
import javax.baja.lonworks.datatypes.BNvConfigData;

public class NetMessageReceiver implements LonListener, NetMessages {
   ServicePin srvcPin = null;
   Object servicePinSync = new Object();
   private BLonNetwork lonworks;
   private boolean firstPass = true;
   private NLonComm lonComm;

   public NetMessageReceiver(BLonNetwork lon) {
      this.lonworks = lon;
   }

   private void startNetwork() {
      if (this.firstPass) {
         this.lonComm = (NLonComm)this.lonworks.lonComm();
         this.register(104, QueryNvConfigRequest.class);
         this.register(107, UpdateNvConfigRequest.class);
         this.register(114, QuerySNVTRequest.class);
         this.register(115, FetchNvRequest.class);
         this.register(108, SetNodeModeRequest.class);
         this.register(112, WinkRequest.class);
         this.register(127, ServicePin.class);
      }

      this.firstPass = false;
   }

   private void register(int c, Class<?> cl) {
      this.lonComm.registerLonListener(this, c, null, cl);
   }

   public void okToReceive() {
      this.startNetwork();
      this.lonworks.log().info("okToReceive");
   }

   @Override
   public void receiveLonMessage(LonMessage msg) {
      switch (msg.getMessageCode()) {
         case 104:
            this.processQueryNvConfig((QueryNvConfigRequest)msg);
         case 105:
         case 106:
         case 109:
         case 110:
         case 111:
         case 113:
         case 116:
         case 117:
         case 118:
         case 119:
         case 120:
         case 121:
         case 122:
         case 123:
         case 124:
         case 125:
         case 126:
         default:
            break;
         case 107:
            this.processUpdateNvConfig((UpdateNvConfigRequest)msg);
            break;
         case 108:
            this.processSetNodeMode((SetNodeModeRequest)msg);
            break;
         case 112:
            this.processWink((WinkRequest)msg);
            break;
         case 114:
            this.processQuerySnvt((QuerySNVTRequest)msg);
            break;
         case 115:
            this.processNvFetch((FetchNvRequest)msg);
            break;
         case 127:
            this.processServicePin((ServicePin)msg);
      }
   }

   private void processQueryNvConfig(QueryNvConfigRequest qnvCnf) {
      int nvIndex = qnvCnf.getNvIndex();
      BINetworkVariable[] nvs = this.lonworks.getLocalLonDevice().getNetworkVariables();
      if (nvIndex < nvs.length && nvs[nvIndex] != null) {
         QueryNvConfigResponse resp = new QueryNvConfigResponse(nvs[nvIndex].getNvConfigData());
         this.lonComm.sendResponse(qnvCnf, resp);
      } else {
         this.lonComm.sendResponse(qnvCnf, new NoDataResponse(8));
      }
   }

   private void processUpdateNvConfig(UpdateNvConfigRequest updNv) {
      int nvIndex = updNv.getNvIndex();
      BNvConfigData nvCfgDat = updNv.getConfigData();
      BINetworkVariable[] nvs = this.lonworks.getLocalLonDevice().getNetworkVariables();
      if (nvIndex < nvs.length && nvs[nvIndex] != null && nvCfgDat.getDirection() == nvs[nvIndex].getNvConfigData().getDirection()) {
         nvs[nvIndex].setNvConfigData(nvCfgDat);
         this.lonComm.sendResponse(updNv, new NoDataResponse(43));
      } else {
         this.lonComm.sendResponse(updNv, new NoDataResponse(11));
      }
   }

   private void processQuerySnvt(QuerySNVTRequest qsnvt) {
      SnvtInfo nvInfo = this.lonworks.getLocalLonDevice().getSnvtInfo();
      byte[] data = nvInfo.getInfoData(qsnvt.getOffset(), qsnvt.getCount());
      QuerySNVTResponse resp = new QuerySNVTResponse(data);
      this.lonComm.sendResponse(qsnvt, resp);
   }

   private void processNvFetch(FetchNvRequest fetchNv) {
      int nvIndex = fetchNv.getNvIndex();
      BINetworkVariable[] nvs = this.lonworks.getLocalLonDevice().getNetworkVariables();
      if (nvIndex < nvs.length && nvs[nvIndex] != null) {
         FetchNvResponse resp = new FetchNvResponse(nvIndex, nvs[nvIndex].getData().toNetBytes());
         this.lonComm.sendResponse(fetchNv, resp);
      } else {
         this.lonComm.sendResponse(fetchNv, new NoDataResponse(19));
      }
   }

   private void processSetNodeMode(SetNodeModeRequest msg) {
      LonMessage rsp = msg.isRequest() ? new LonMessage() : null;

      try {
         switch (msg.getMode()) {
            case 0:
               this.lonComm.sendLocalCommand(128);
               break;
            case 1:
               this.lonComm.sendLocalCommand(112);
         }

         if (rsp == null) {
            return;
         }

         rsp.setMessageCode(44);
      } catch (Throwable var4) {
         if (rsp != null) {
            rsp.setMessageCode(12);
         }
      }

      this.lonComm.sendResponse(msg, rsp);
   }

   private void processWink(WinkRequest wink) {
      LonMessage rsp = wink.isRequest() ? new LonMessage() : null;
      switch (wink.getSubCommand()) {
         case 0:
            System.out.println("Wink message received.");
            this.lonworks.getLocalLonDevice().fireWink(null);
            if (rsp != null) {
               rsp.setMessageCode(48);
            }
            break;
         case 1:
         default:
            if (rsp != null) {
               rsp.setMessageCode(16);
            }
      }

      if (rsp != null) {
         this.lonComm.sendResponse(wink, rsp);
      }
   }

   public ServicePin getServicePin(int timeToWait) throws InterruptedException {
      synchronized (this.servicePinSync) {
         if (this.srvcPin == null) {
            this.servicePinSync.wait(timeToWait);
         }

         return this.srvcPin;
      }
   }

   public ServicePin getServicePin() {
      synchronized (this.servicePinSync) {
         if (this.srvcPin == null) {
            try {
               this.servicePinSync.wait();
            } catch (Exception var4) {
            }
         }

         return this.srvcPin;
      }
   }

   public void cancelServicePin() {
      synchronized (this.servicePinSync) {
         this.servicePinSync.notify();
      }
   }

   public void clearServicePin() {
      synchronized (this.servicePinSync) {
         this.srvcPin = null;
      }
   }

   private void processServicePin(ServicePin msg) {
      synchronized (this.servicePinSync) {
         this.srvcPin = msg;
         this.lonworks.netmgmt().receiveServicePin(msg);
         this.servicePinSync.notify();
      }

      this.lonworks.log().info("Receive Service pin." + msg + "\n");
   }
}
