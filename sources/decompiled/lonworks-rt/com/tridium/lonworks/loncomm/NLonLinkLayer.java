package com.tridium.lonworks.loncomm;

import com.tridium.lonworks.netmessages.QueryStatusRequest;
import com.tridium.lonworks.util.NmUtil;
import com.tridium.platLon.BLonPlatformService;
import com.tridium.sys.station.Station;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.lonworks.BLonNetwork;
import javax.baja.lonworks.LonComm;
import javax.baja.lonworks.LonException;
import javax.baja.lonworks.datatypes.BLocal;
import javax.baja.lonworks.enums.BLonServiceType;
import javax.baja.lonworks.io.AppBuffer;
import javax.baja.lonworks.io.LonLinkLayer;
import javax.baja.space.BComponentSpace;
import javax.baja.sys.BComponent;
import javax.baja.sys.Sys;

public final class NLonLinkLayer implements LonLinkLayer, ListenerSupport {
   private String lonPort = "";
   private static Hashtable<String, NLonLinkLayer> devNameHash = new Hashtable<>();
   private NLonLinkLayer.AppBufferReceive appBuffRcv;
   private NLonLinkLayer.LonReceiveDriver rcvDriver;
   private NLonLinkLayer.LonTransmitDriver xmitDriver;
   private Thread rcvThread = null;
   private Thread appThread = null;
   private Thread xmitThread = null;
   private BLonPlatformService driver;
   private int ldvHandle;
   private BLonNetwork lonworks;
   private NLonComm lonComm;
   private boolean done = true;
   private LonLinkListenerRegistry lstnrs = null;

   public NLonLinkLayer(LonComm lonComm, BLonNetwork lonworks) {
      this.lonworks = lonworks;
      this.lonComm = (NLonComm)lonComm;
      this.lstnrs = new LonLinkListenerRegistry(lonworks);
   }

   @Override
   public void verifySettings() throws Exception {
      this.init();
   }

   private void init() throws Exception {
      String devName = this.lonworks.getLonCommConfig().getDeviceName();
      verifyUnique(devName, this);
      if (!devName.equals(this.lonPort)) {
         BComponentSpace cs = this.lonworks.getComponentSpace();
         if (cs != null && cs == Station.space) {
            this.driver = (BLonPlatformService)Sys.getService(BLonPlatformService.TYPE);
            if (this.driver == null) {
               throw new LonException("No LonPlatformService available");
            } else {
               try {
                  this.ldvHandle = this.driver.driverInit(devName);
                  this.lonPort = devName;
               } catch (Exception var7) {
                  BComponent[] a = Sys.getServices(BLonNetwork.TYPE);

                  for (int i = 0; i < a.length; i++) {
                     BLonNetwork net = (BLonNetwork)a[i];
                     if (net != this.lonworks && net.getLonCommConfig().getDeviceName().equals(devName)) {
                        this.lonworks
                           .log()
                           .severe(
                              "Unable to initialize driver because lon port in use by "
                                 + net.getDisplayName(null)
                                 + ". Change loncomm deviceName and restart station"
                           );
                        break;
                     }
                  }

                  throw new LonException("Unable to initialize local lon port {" + devName + "}", var7);
               }
            }
         }
      }
   }

   private static void verifyUnique(String devName, NLonLinkLayer nlon) throws Exception {
      NLonLinkLayer o = devNameHash.get(devName);
      if (o != null && (!o.lonworks.isMounted() || o.lonworks.isDisabled())) {
         devNameHash.remove(devName);
         o = null;
      }

      if (o != null && o != nlon) {
         throw new LonException("deviceName must be unique");
      } else {
         String oldName = nlon.lonPort;
         if (!devName.equals(oldName) && oldName.length() > 0) {
            devNameHash.remove(oldName);
         }

         if (o == null) {
            devNameHash.put(devName, nlon);
         }
      }
   }

   @Override
   public void start() throws Exception {
      this.init();
      this.done = false;
      this.appBuffRcv = new NLonLinkLayer.AppBufferReceive();
      this.appThread = new Thread(this.appBuffRcv, this.lonComm.lonNetwork().getLogName() + ".AppBufferRcv");
      this.appThread.start();
      this.appThread.setPriority(5);
      this.rcvDriver = new NLonLinkLayer.LonReceiveDriver();
      this.rcvThread = new Thread(this.rcvDriver, this.lonComm.lonNetwork().getLogName() + ".RcvDriver");
      this.rcvThread.start();
      this.rcvThread.setPriority(7);
      this.xmitDriver = new NLonLinkLayer.LonTransmitDriver();
      this.xmitThread = new Thread(this.xmitDriver, this.lonComm.lonNetwork().getLogName() + ".XmitDriver");
      this.xmitThread.start();
      this.xmitThread.setPriority(5);
   }

   @Override
   public void stop() {
      this.done = true;
      if (this.xmitThread != null) {
         this.xmitThread.interrupt();
      }

      if (this.rcvThread != null) {
         this.rcvThread.interrupt();
      }

      if (this.appThread != null) {
         this.appThread.interrupt();
      }

      try {
         NAppBuffer appReq = NAppBuffer.makeAppBuffer();
         appReq.setServiceType(BLonServiceType.request);
         appReq.setDestAddress(BLocal.local);
         appReq.setMessage(new QueryStatusRequest());
         this.driver.write(this.ldvHandle, appReq.getWriteBuffer(), appReq.getWriteBufferLen());
      } catch (Exception var2) {
         var2.printStackTrace();
      }

      this.rcvDriver.finished();
      this.driver.close(this.ldvHandle);
   }

   @Override
   public void sendLonMessage(AppBuffer appBuffer) {
      if (!this.done) {
         this.xmitDriver.sendToDriver((NAppBuffer)appBuffer);
      }
   }

   private Logger log() {
      return this.lonworks.log();
   }

   @Override
   public LonLinkListenerRegistry getLonLinkListenerRegistry() {
      return this.lstnrs;
   }

   private class AppBufferReceive implements Runnable {
      LinkedQueue rcvQueue = new LinkedQueue();

      private AppBufferReceive() {
      }

      @Override
      public void run() {
         while (!NLonLinkLayer.this.done) {
            try {
               NAppBuffer newMsg = (NAppBuffer)this.rcvQueue.dequeue();
               if (newMsg != null) {
                  NLonLinkLayer.this.lonComm.receiveLonMessage(newMsg);
               }
            } catch (ThreadDeath var2) {
               NLonLinkLayer.this.done = true;
            } catch (Throwable var3) {
               var3.printStackTrace();
               NLonLinkLayer.this.lonworks.log().severe("Exception caught in AppBufferReceive. " + var3);
            }
         }
      }

      private void rcvAppBuffer(NAppBuffer rcvMessage) {
         this.rcvQueue.enqueue(rcvMessage);
      }
   }

   private class LonReceiveDriver implements Runnable {
      boolean finished = false;

      private LonReceiveDriver() {
      }

      @Override
      public void run() {
         NAppBuffer newMsg = NAppBuffer.makeAppBuffer();

         while (!NLonLinkLayer.this.done) {
            try {
               byte[] netBytes = newMsg.getReadBuffer();
               if (!NLonLinkLayer.this.driver.read(NLonLinkLayer.this.ldvHandle, netBytes)) {
                  NmUtil.wait(20);
               } else {
                  NLonLinkLayer.this.lstnrs.listenerReceive(newMsg);
                  if (NLonLinkLayer.this.log().isLoggable(Level.FINE) || NLonLinkLayer.this.lonComm.getLinkDebug()) {
                     NLonLinkLayer.this.lstnrs.writeLinkDebug("Link recv: ", netBytes, (netBytes[1] & 255) + 2);
                  }

                  byte len = netBytes[1];
                  if (len > 5 && netBytes[4] != len - 14) {
                     netBytes[4] = (byte)(len - 14);
                  }

                  if (newMsg.isResponse()) {
                     NLonLinkLayer.this.lonComm.handleResponseMessage(newMsg);
                  } else {
                     NLonLinkLayer.this.appBuffRcv.rcvAppBuffer(newMsg);
                  }

                  newMsg = NAppBuffer.makeAppBuffer();
               }
            } catch (ThreadDeath var6) {
               NLonLinkLayer.this.done = true;
            } catch (Throwable var7) {
               NLonLinkLayer.this.lonworks.setFaultCause(var7.getMessage());
               NLonLinkLayer.this.lonworks.log().log(Level.SEVERE, "NLonLinkLayer.LonReceiveDriver error", var7);
               NmUtil.wait(200);
            }
         }

         this.finished = true;
         synchronized (this) {
            this.notifyAll();
         }
      }

      synchronized void finished() {
         if (!this.finished) {
            try {
               this.wait(500L);
            } catch (Exception var2) {
            }
         }
      }
   }

   private class LonTransmitDriver implements Runnable {
      LinkedQueue sendQueue = new LinkedQueue();

      private LonTransmitDriver() {
      }

      @Override
      public void run() {
         while (!NLonLinkLayer.this.done) {
            NAppBuffer newMsg = (NAppBuffer)this.sendQueue.dequeue();
            if (newMsg != null) {
               try {
                  if (NLonLinkLayer.this.log().isLoggable(Level.FINE) || NLonLinkLayer.this.lonComm.getLinkDebug()) {
                     byte[] a = newMsg.getWriteBuffer();
                     NLonLinkLayer.this.lstnrs.writeLinkDebug("Link send: ", a, (a[1] & 255) + 2);
                  }

                  NLonLinkLayer.this.lstnrs.listenerSend(newMsg);
                  NLonLinkLayer.this.driver.write(NLonLinkLayer.this.ldvHandle, newMsg.getWriteBuffer(), newMsg.getWriteBufferLen());
               } catch (ThreadDeath var5) {
                  NLonLinkLayer.this.done = true;
               } catch (Throwable var6) {
                  Throwable e = var6;

                  try {
                     NLonLinkLayer.this.lonComm.handleFailedTransmit(newMsg, new LonException("Failed transmit to lonDriver.", e));
                  } catch (Throwable var4) {
                     var4.printStackTrace();
                     NLonLinkLayer.this.lonworks.log().log(Level.SEVERE, "Unable to handle failed transmit in NLonLinkLayer ", var4);
                  }
               }

               if (newMsg.isNoTransaction()) {
                  newMsg.releaseAppBuffer();
               }
            }
         }
      }

      private void sendToDriver(NAppBuffer sendMessage) {
         this.sendQueue.enqueue(sendMessage);
      }
   }
}
