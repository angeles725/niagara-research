package com.tridium.lonworks.loncomm;

import com.tridium.lonworks.enums.BLonCompletionCode;
import com.tridium.lonworks.netmessages.UnprocessedNV;
import com.tridium.lonworks.util.NmUtil;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;
import java.util.logging.Level;
import javax.baja.lonworks.BLonNetwork;
import javax.baja.lonworks.InvalidResponseException;
import javax.baja.lonworks.LonComm;
import javax.baja.lonworks.LonException;
import javax.baja.lonworks.LonListener;
import javax.baja.lonworks.LonMessage;
import javax.baja.lonworks.datatypes.BSubnetNode;
import javax.baja.lonworks.datatypes.LonAddress;
import javax.baja.lonworks.enums.BLonServiceType;
import javax.baja.lonworks.io.LonLinkLayer;
import javax.baja.spy.SpyWriter;
import javax.baja.sys.BajaRuntimeException;

public class NLonComm implements LonComm {
   protected BLonNetwork lonworks;
   LonLinkLayer linkLayer;
   private LonTransactionManager transactionManager;
   private boolean serviceStarted = false;
   private int workingDomain = 0;
   private HashMap<Integer, Vector<NLonComm.ListenerData>> listners = new HashMap<>(32);
   private LonListener nvListner = null;

   public NLonComm(BLonNetwork lon) {
      this.lonworks = lon;
      this.transactionManager = new LonTransactionManager(this);
      this.linkLayer = lon.getLonCommConfig().makeLonLinkLayer(this, lon);
   }

   public void start() throws Exception {
      this.updateWorkingDomain();
      this.linkLayer.start();
      this.serviceStarted = true;
      this.transactionManager.start();
   }

   public void stop() {
      if (this.serviceStarted) {
         this.serviceStarted = false;
         this.linkLayer.stop();
         this.transactionManager.stop();
      }
   }

   public void verifySettings() throws Exception {
      this.linkLayer.verifySettings();
   }

   @Override
   public LonMessage sendRequest(LonAddress destAddr, LonMessage netRequest) throws LonException {
      NAppBuffer appReq = NAppBuffer.makeAppBuffer();
      appReq.setServiceType(BLonServiceType.request);
      appReq.setAuthenticate(netRequest.isAuthenticate());
      appReq.setDomainIndex(this.workingDomain);
      appReq.setDestAddress(destAddr);
      appReq.setMessage(netRequest);
      LonException e = null;
      NAppBuffer appResp = null;
      LonMessage respMsg = null;
      boolean firstPass = true;
      boolean retry = false;

      try {
         while (firstPass || retry) {
            firstPass = false;
            appResp = this.doLonCommSendRequest(appReq, -1);
            if (appResp == null) {
               e = new LonException("Message failed to send.");
            } else if (appResp.exception != null) {
               e = new LonException("SendRequest failed", appResp.exception);
            } else {
               try {
                  respMsg = appResp.makeResponse(netRequest);
                  retry = false;
               } catch (InvalidResponseException var10) {
                  retry = !retry;
                  if (retry) {
                     appResp.releaseAppBuffer();
                  } else {
                     e = var10;
                  }
               }
            }
         }
      } catch (LonException var11) {
         e = var11;
      }

      appReq.releaseAppBuffer();
      if (appResp != null) {
         appResp.releaseAppBuffer();
      }

      if (e != null) {
         throw e;
      } else {
         return respMsg;
      }
   }

   public NAppBuffer doLonCommSendRequest(NAppBuffer appBuffer, int tag) throws LonException {
      if (!this.serviceStarted) {
         this.lonCommNotStarted();
      }

      appBuffer.setDefaultTimers(this.lonworks);
      LonTransaction transaction;
      if (tag == 15) {
         transaction = this.transactionManager.getLonTransactionTag15(appBuffer);
      } else {
         transaction = this.transactionManager.getLonTransaction(appBuffer);
      }

      if (transaction == null) {
         return null;
      } else {
         synchronized (transaction) {
            this.linkLayer.sendLonMessage(appBuffer);

            try {
               transaction.wait();
               transaction.setComplete(true);
            } catch (InterruptedException var7) {
            }
         }

         NAppBuffer response = transaction.getResponseMessage();
         LonException e = transaction.getException();
         this.transactionManager.freeLonTransaction(transaction);
         if (response == null && e != null) {
            throw new LonException("send failed", e);
         } else {
            if (e != null) {
               response.exception = e;
            }

            return response;
         }
      }
   }

   @Override
   public void sendResponse(LonMessage origMsg, LonMessage netResponse) {
      if (!this.serviceStarted) {
         this.lonCommNotStarted();
      }

      NAppBuffer respMsg = NAppBuffer.makeAppBuffer();
      respMsg.setTag(origMsg.getTag());
      respMsg.setServiceType(BLonServiceType.request);
      respMsg.setPriority(origMsg.isPriority());
      respMsg.setResp(true);
      respMsg.setDestAddress(origMsg.getSourceAddress());
      respMsg.setMessage(netResponse);
      this.doSendResponse(respMsg);
   }

   public void doSendResponse(NAppBuffer respMsg) {
      respMsg.setNoTransaction(true);
      this.linkLayer.sendLonMessage(respMsg);
   }

   @Override
   public void sendAcked(LonAddress destAddr, LonMessage netRequest) throws LonException {
      NAppBuffer appReq = NAppBuffer.makeAppBuffer();
      appReq.setServiceType(BLonServiceType.acked);
      appReq.setAuthenticate(netRequest.isAuthenticate());
      appReq.setDomainIndex(this.workingDomain);
      appReq.setDestAddress(destAddr);
      appReq.setMessage(netRequest);
      this.sendNormal(appReq);
   }

   @Override
   public void sendUnacknowledged(LonAddress destAddr, LonMessage netRequest) throws LonException {
      NAppBuffer appReq = NAppBuffer.makeAppBuffer();
      appReq.setQueue(4);
      appReq.setServiceType(BLonServiceType.unacked);
      appReq.setAuthenticate(false);
      appReq.setDomainIndex(this.workingDomain);
      appReq.setDestAddress(destAddr);
      appReq.setMessage(netRequest);
      this.sendNormal(appReq);
   }

   @Override
   public void sendUnackRepeat(LonAddress destAddr, LonMessage netRequest) throws LonException {
      NAppBuffer appReq = NAppBuffer.makeAppBuffer();
      appReq.setQueue(2);
      appReq.setServiceType(BLonServiceType.unackedRpt);
      appReq.setAuthenticate(false);
      appReq.setDomainIndex(this.workingDomain);
      appReq.setDestAddress(destAddr);
      appReq.setMessage(netRequest);
      this.sendNormal(appReq);
   }

   public void sendNormal(NAppBuffer appBuffer) throws LonException {
      LonException e = null;
      NAppBuffer resp = null;

      try {
         resp = this.doLonCommSendNormal(appBuffer);
         if (resp != null && resp.exception != null) {
            e = new LonException("sendNormal failed", resp.exception);
         }
      } catch (LonException var5) {
         e = var5;
      }

      if (!appBuffer.isNoTransaction()) {
         appBuffer.releaseAppBuffer();
      }

      if (resp != null) {
         resp.releaseAppBuffer();
      }

      if (e != null) {
         throw e;
      }
   }

   public NAppBuffer doLonCommSendNormal(NAppBuffer appBuffer) throws LonException {
      if (!this.serviceStarted) {
         this.lonCommNotStarted();
      }

      appBuffer.setDefaultTimers(this.lonworks);
      if (appBuffer.isLocalAddress()) {
         appBuffer.setNoTransaction(true);
         this.linkLayer.sendLonMessage(appBuffer);
         return null;
      } else {
         LonTransaction transaction = this.transactionManager.getLonTransaction(appBuffer);
         if (transaction == null) {
            return null;
         } else {
            synchronized (transaction) {
               this.linkLayer.sendLonMessage(appBuffer);

               try {
                  transaction.wait();
                  transaction.setComplete(true);
               } catch (InterruptedException var6) {
               }
            }

            NAppBuffer response = transaction.getResponseMessage();
            LonException e = transaction.getException();
            this.transactionManager.freeLonTransaction(transaction);
            if (response == null && e != null) {
               throw new LonException("sendNormal failed", e);
            } else {
               if (e != null) {
                  response.exception = e;
               }

               return response;
            }
         }
      }
   }

   public void sendLocalCommand(int command) throws LonException {
      if (!this.serviceStarted) {
         this.lonCommNotStarted();
      }

      NAppBuffer appBuffer = NAppBuffer.makeAppBuffer();
      appBuffer.setLocalCommand(command);
      appBuffer.setNoTransaction(true);
      this.linkLayer.sendLonMessage(appBuffer);
   }

   public void receiveLonMessage(NAppBuffer appBuffer) {
      if (appBuffer.isIncoming()) {
         try {
            if ((appBuffer.getMessageCode() & 128) == 128) {
               this.routeToNvListener(appBuffer);
            } else {
               this.routeToListeners(appBuffer);
            }
         } catch (LonException var3) {
            this.lonworks.log().log(Level.SEVERE, "Unable to process received message. \n" + appBuffer + "\n", (Throwable)var3);
         }
      } else if (appBuffer.getCommand() < 80) {
         this.lonworks.log().severe("Unknown app buffer msg type. \n" + appBuffer);
      }

      appBuffer.releaseAppBuffer();
   }

   protected void handleFailedTransmit(NAppBuffer appBuff, LonException e) {
      if (!appBuff.isNoTransaction()) {
         int tag = appBuff.getTransactionTag();
         LonTransaction transaction = this.getTransactionManager().getLonTransactionMatch(tag);
         synchronized (transaction) {
            transaction.setResponseMessage(null);
            transaction.setException(e);
            transaction.notify();
         }
      }
   }

   public void handleResponseMessage(NAppBuffer appBuffer) {
      int tag = appBuffer.getTransactionTag();
      LonTransaction transaction = this.getTransactionManager().getLonTransactionMatch(tag);
      boolean validTransaction = false;
      synchronized (transaction) {
         if (transaction.isUsed()) {
            validTransaction = true;
            transaction.addResponseMessage(appBuffer);
            BLonCompletionCode complCode = appBuffer.getComplCode();
            if (complCode == BLonCompletionCode.notComp) {
               if (transaction.isLocal()) {
                  transaction.notify();
               }
            } else if (complCode == BLonCompletionCode.succeeds) {
               transaction.notify();
            } else if (complCode == BLonCompletionCode.fails) {
               LonException ae = appBuffer.exception != null
                  ? appBuffer.exception
                  : new LonException(Integer.toString(tag, 16) + "Failed completion event rcvd from Neuron - possible timeout before response received ");
               transaction.setException(ae);
               transaction.notify();
            }
         }
      }

      if (!validTransaction) {
         this.lonworks.log().warning("Received response message with no matching transaction.\n" + appBuffer);
         appBuffer.releaseAppBuffer();
      }
   }

   private void lonCommNotStarted() {
      throw new BajaRuntimeException("LonComm not started.");
   }

   public void registerNvListener(LonListener listner) {
      this.nvListner = listner;
   }

   private void routeToNvListener(NAppBuffer appBuffer) throws LonException {
      if (this.nvListner != null) {
         LonMessage msg = appBuffer.getLonMessage(UnprocessedNV.class);
         this.nvListner.receiveLonMessage(msg);
      }
   }

   @Override
   public void registerLonListener(LonListener listner, int msgCode, BSubnetNode address, Class<?> messageClass) {
      synchronized (this.listners) {
         if (msgCode >= 0 && msgCode <= 127) {
            Vector<NLonComm.ListenerData> v = this.listners.get(msgCode);
            if (v == null) {
               v = new Vector<>(4);
               this.listners.put(msgCode, v);
            }

            for (int i = 0; i < v.size(); i++) {
               NLonComm.ListenerData ld = v.elementAt(i);
               if (listner == ld.listner && (ld.address == null || address == ld.address)) {
                  return;
               }
            }

            v.addElement(new NLonComm.ListenerData(listner, address, messageClass));
         } else {
            throw new IllegalArgumentException("Invalid message code " + msgCode + ", must be in range 0 to 127");
         }
      }
   }

   @Override
   public void unregisterLonListener(LonListener listner, int msgCode, BSubnetNode address) {
      synchronized (this.listners) {
         if (msgCode == -1) {
            Iterator<Vector<NLonComm.ListenerData>> itr = this.listners.values().iterator();

            while (itr.hasNext()) {
               this.unregister(itr.next(), listner, address);
            }
         } else {
            Vector<NLonComm.ListenerData> v = this.listners.get(msgCode);
            if (v == null) {
               return;
            }

            this.unregister(v, listner, address);
         }
      }
   }

   private void unregister(Vector<NLonComm.ListenerData> v, LonListener listner, BSubnetNode address) {
      for (int i = v.size() - 1; i >= 0; i--) {
         NLonComm.ListenerData ld = v.elementAt(i);
         if (listner == ld.listner && (address == null || address == ld.address)) {
            v.removeElementAt(i);
         }
      }
   }

   private void routeToListeners(NAppBuffer appBuffer) throws LonException {
      boolean routed = false;
      synchronized (this.listners) {
         Vector<NLonComm.ListenerData> v = this.listners.get(appBuffer.getMessageCode());
         if (v != null) {
            for (int i = 0; i < v.size(); i++) {
               NLonComm.ListenerData ld = v.elementAt(i);
               if (ld.address == null || ld.address.equals(appBuffer.getSourceAddress())) {
                  LonMessage msg = appBuffer.getLonMessage(ld.messageClass);
                  ld.listner.receiveLonMessage(msg);
                  routed = true;
               }
            }
         }
      }

      if (!routed) {
         this.lonworks.log().warning("No listener for received message:" + appBuffer);
         if (appBuffer.isRequest()) {
            NmUtil.sendFailedResponse(this, appBuffer.getLonMessage(null));
         }
      }
   }

   private LonTransactionManager getTransactionManager() {
      return this.transactionManager;
   }

   public boolean getLinkDebug() {
      return this.lonworks.getLonCommConfig().getLinkDebug();
   }

   @Override
   public BLonNetwork lonNetwork() {
      return this.lonworks;
   }

   public void updateWorkingDomain() {
      this.workingDomain = this.lonworks.getLocalLonDevice().getDeviceData().getWorkingDomain();
   }

   public void spy(SpyWriter out) throws Exception {
      this.transactionManager.spy(out);
   }

   private static class ListenerData {
      LonListener listner;
      BSubnetNode address;
      Class<?> messageClass;

      ListenerData(LonListener l, BSubnetNode sn, Class<?> cl) {
         this.listner = l;
         this.address = sn;
         this.messageClass = cl;
      }
   }
}
