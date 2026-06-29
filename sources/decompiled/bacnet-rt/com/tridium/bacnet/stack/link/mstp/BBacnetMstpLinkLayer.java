package com.tridium.bacnet.stack.link.mstp;

import com.tridium.bacnet.enums.BBacnetMstpBaudRate;
import com.tridium.bacnet.enums.BBacnetMstpUsageTimeout;
import com.tridium.bacnet.stack.BacnetInputStream;
import com.tridium.bacnet.stack.link.BBacnetLinkLayer;
import com.tridium.bacnet.stack.network.NetworkPdu;
import com.tridium.platMstp.BBacnetMstpPlatformService;
import com.tridium.platMstp.BBacnetMstpPlatformServiceEmstp;
import com.tridium.platMstp.EmstpStats;
import com.tridium.platMstp.MstpListener;
import java.io.ByteArrayOutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.bacnet.BBacnetNetwork;
import javax.baja.bacnet.export.BLocalBacnetDevice;
import javax.baja.license.Feature;
import javax.baja.license.FeatureNotLicensedException;
import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraActions;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.util.ByteArrayUtil;
import javax.baja.nre.util.TextUtil;
import javax.baja.serial.BISerialService;
import javax.baja.serial.PortNotFoundException;
import javax.baja.spy.SpyWriter;
import javax.baja.sys.Action;
import javax.baja.sys.BComponent;
import javax.baja.sys.BFacets;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "portName",
      type = "String",
      defaultValue = "COM1",
      flags = 64
   ), @NiagaraProperty(
      name = "mstpTrunk",
      type = "int",
      defaultValue = "0",
      flags = 5
   ), @NiagaraProperty(
      name = "baudRate",
      type = "BBacnetMstpBaudRate",
      defaultValue = "BBacnetMstpBaudRate.baud_9600"
   ), @NiagaraProperty(
      name = "mstpAddress",
      type = "int",
      defaultValue = "0",
      facets = {@Facet("BFacets.makeInt(0, 127)")}
   ), @NiagaraProperty(
      name = "maxMaster",
      type = "int",
      defaultValue = "127",
      facets = {@Facet("BFacets.makeInt(0, 127)")}
   ), @NiagaraProperty(
      name = "maxInfoFrames",
      type = "int",
      defaultValue = "20",
      facets = {@Facet("BFacets.makeInt(1,100)")}
   ), @NiagaraProperty(
      name = "supportExtendedFrames",
      type = "boolean",
      defaultValue = "false"
   ), @NiagaraProperty(
      name = "usageTimeout",
      type = "BBacnetMstpUsageTimeout",
      defaultValue = "BBacnetMstpUsageTimeout.ms_20",
      flags = 4
   ), @NiagaraProperty(
      name = "txThrottle",
      type = "int",
      defaultValue = "10",
      flags = 4,
      facets = {@Facet("BFacets.makeInt(0,20)")}
   )})
@NiagaraActions({@NiagaraAction(
      name = "resetCounters",
      flags = 20
   ), @NiagaraAction(
      name = "getStatistics",
      flags = 20
   )})
public class BBacnetMstpLinkLayer extends BBacnetLinkLayer implements MstpListener {
   public static final Property portName = newProperty(64, "COM1", null);
   public static final Property mstpTrunk = newProperty(5, 0, null);
   public static final Property baudRate = newProperty(0, BBacnetMstpBaudRate.baud_9600, null);
   public static final Property mstpAddress = newProperty(0, 0, BFacets.makeInt(0, 127));
   public static final Property maxMaster = newProperty(0, 127, BFacets.makeInt(0, 127));
   public static final Property maxInfoFrames = newProperty(0, 20, BFacets.makeInt(1, 100));
   public static final Property supportExtendedFrames = newProperty(0, false, null);
   public static final Property usageTimeout = newProperty(4, BBacnetMstpUsageTimeout.ms_20, null);
   public static final Property txThrottle = newProperty(4, 10, BFacets.makeInt(0, 20));
   public static final Action resetCounters = newAction(20, null);
   public static final Action getStatistics = newAction(20, null);
   public static final Type TYPE = Sys.loadType(BBacnetMstpLinkLayer.class);
   private static boolean licenseChecked = false;
   protected static int[] trunks;
   public static final int MAX_NPDU_LENGTH = 501;
   public static final int DEFAULT_MAX_APDU_LENGTH_ACCEPTED = 480;
   public static final int MAX_EXT_NPDU_LENGTH = 1497;
   public static final int DEFAULT_MAX_EXT_APDU_LENGTH_ACCEPTED = 1476;
   public static final byte MSTP_BROADCAST_MAC_ADDRESS = -1;
   public static final String MSTP_TRUNK_PREFIX = "/dev/mstp";
   private final ByteArrayOutputStream os = new ByteArrayOutputStream();
   private boolean useCoprocessor = true;
   private Logger logger;
   protected int handle = -1;
   protected boolean commStarted = false;
   protected BComponent portComp;

   public String getPortName() {
      return this.getString(portName);
   }

   public void setPortName(String v) {
      this.setString(portName, v, null);
   }

   public int getMstpTrunk() {
      return this.getInt(mstpTrunk);
   }

   public void setMstpTrunk(int v) {
      this.setInt(mstpTrunk, v, null);
   }

   public BBacnetMstpBaudRate getBaudRate() {
      return (BBacnetMstpBaudRate)this.get(baudRate);
   }

   public void setBaudRate(BBacnetMstpBaudRate v) {
      this.set(baudRate, v, null);
   }

   public int getMstpAddress() {
      return this.getInt(mstpAddress);
   }

   public void setMstpAddress(int v) {
      this.setInt(mstpAddress, v, null);
   }

   public int getMaxMaster() {
      return this.getInt(maxMaster);
   }

   public void setMaxMaster(int v) {
      this.setInt(maxMaster, v, null);
   }

   public int getMaxInfoFrames() {
      return this.getInt(maxInfoFrames);
   }

   public void setMaxInfoFrames(int v) {
      this.setInt(maxInfoFrames, v, null);
   }

   public boolean getSupportExtendedFrames() {
      return this.getBoolean(supportExtendedFrames);
   }

   public void setSupportExtendedFrames(boolean v) {
      this.setBoolean(supportExtendedFrames, v, null);
   }

   public BBacnetMstpUsageTimeout getUsageTimeout() {
      return (BBacnetMstpUsageTimeout)this.get(usageTimeout);
   }

   public void setUsageTimeout(BBacnetMstpUsageTimeout v) {
      this.set(usageTimeout, v, null);
   }

   public int getTxThrottle() {
      return this.getInt(txThrottle);
   }

   public void setTxThrottle(int v) {
      this.setInt(txThrottle, v, null);
   }

   public void resetCounters() {
      this.invoke(resetCounters, null, null);
   }

   public void getStatistics() {
      this.invoke(getStatistics, null, null);
   }

   @Override
   public Type getType() {
      return TYPE;
   }

   private void checkLicense() {
      try {
         if (!licenseChecked) {
            Feature mstp = Sys.getLicenseManager().getFeature("tridium", "mstp");
            if (mstp == null) {
               throw new FeatureNotLicensedException("MSTP not licensed");
            }

            mstp.check();
            String portLimit = mstp.get("port.limit");
            int numTrunks = 6;
            if (portLimit != null && !"none".equals(TextUtil.toLowerCase(portLimit))) {
               numTrunks = Integer.parseInt(portLimit);
            }

            if (numTrunks > 6) {
               numTrunks = 6;
            }

            trunks = new int[numTrunks];
            licenseChecked = true;
         }
      } catch (Exception var4) {
         this.getNetworkPort().fault("BACnet MSTP not licensed");
      }
   }

   private int nextOpenTrunk() {
      if (trunks == null) {
         return 0;
      } else {
         for (int i = 0; i < trunks.length; i++) {
            if (trunks[i] == 0) {
               trunks[i] = i + 1;
               return i + 1;
            }
         }

         this.getNetworkPort().fault("Exceeded mstp port.limit");
         return 0;
      }
   }

   @Override
   public void checkFatalFault() {
      try {
         this.checkLicense();
         this.setMstpTrunk(this.nextOpenTrunk());
      } catch (Exception var2) {
         this.getNetworkPort().fault(var2.toString());
      }
   }

   public void started() throws Exception {
      super.started();
      this.logger = Logger.getLogger("bacnet.link.mstp" + this.getMstpTrunk());
      this.setUseCoprocessor(this.getPlatformService().getUseCoprocessor());
   }

   public void stopped() throws Exception {
      this.setUseCoprocessor(this.getPlatformService().getUseCoprocessor());
      super.stopped();
      if (this.getMstpTrunk() > 0 && trunks != null) {
         trunks[this.getMstpTrunk() - 1] = 0;
      }
   }

   public void changed(Property p, Context cx) {
      super.changed(p, cx);
      if (this.isRunning()) {
         if (p.equals(portName)) {
            try {
               if (this.getNetworkPort().getEnabled()) {
                  this.linkCommStop();
                  this.linkCommStart();
               }
            } catch (Exception var5) {
               this.logger.warning("Exception changing BACnet MS/TP port name:" + this.getPortName());
               this.getNetworkPort().fault(var5.toString());
            }
         } else if (p.equals(mstpTrunk)) {
            try {
               this.logger = Logger.getLogger("bacnet.link.mstp" + this.getMstpTrunk());
               if (this.getNetworkPort().getEnabled()) {
                  this.linkCommStop();
                  this.linkCommStart();
               }
            } catch (Exception var4) {
               this.logger.warning("Exception changing BACnet MS/TP trunk:" + var4);
               this.getNetworkPort().fault(var4.toString());
            }
         } else if (p.equals(baudRate)) {
            if (this.commStarted) {
               this.getPlatformService().setBaudRate(this.handle, this.getBaudRate().getOrdinal());
            }
         } else if (p.equals(mstpAddress)) {
            if (this.commStarted) {
               this.getPlatformService().setAddress(this.handle, this.getMstpAddress());
            }
         } else if (p.equals(maxMaster)) {
            if (this.commStarted) {
               this.getPlatformService().setMaxMaster(this.handle, this.getMaxMaster());
            }
         } else if (p.equals(maxInfoFrames)) {
            if (this.commStarted) {
               this.getPlatformService().setMaxInfoFrames(this.handle, this.getMaxInfoFrames());
            }
         } else if (p.equals(supportExtendedFrames)) {
            if (this.commStarted) {
               BLocalBacnetDevice local = BBacnetNetwork.localDevice();
               local.setMaxAPDULengthAccepted(this.getMaxAPDULengthAccepted());
            }
         } else if (p.equals(usageTimeout)) {
            if (this.commStarted) {
               this.getPlatformService().setParameter(this.handle, "usageTimeout", Integer.toString(this.getUsageTimeout().getOrdinal()));
            }
         } else if (p.equals(txThrottle) && this.commStarted) {
            this.getPlatformService().setParameter(this.handle, "txThrottle", Integer.toString(this.getTxThrottle()));
         }
      }
   }

   public String toString(Context cx) {
      StringBuilder sb = new StringBuilder();
      sb.append("MAC ").append(this.getMstpAddress()).append(" on ").append(this.getPortName()).append(" at " + this.getBaudRate());
      return sb.toString();
   }

   public void doResetCounters() {
      if (this.commStarted) {
         this.getPlatformService().setParameter(this.handle, "resetCounters", "all");
      }
   }

   public void doGetStatistics() {
      if (this.commStarted && this.getUseCoprocessor()) {
         this.getPlatformService().setParameter(this.handle, "getStatistics", "all");
      }
   }

   public EmstpStats getEmstpStats() {
      if (this.getUseCoprocessor()) {
         BBacnetMstpPlatformServiceEmstp service = (BBacnetMstpPlatformServiceEmstp)this.getPlatformService();
         return service.getStatistics(this.handle);
      } else {
         return null;
      }
   }

   public void setUseCoprocessor(boolean useCoprocessor) {
      this.useCoprocessor = useCoprocessor;
   }

   public boolean getUseCoprocessor() {
      return this.useCoprocessor;
   }

   public BBacnetMstpPlatformService getPlatformService() {
      try {
         BBacnetMstpPlatformService platSvc = (BBacnetMstpPlatformService)Sys.getService(BBacnetMstpPlatformService.TYPE);
         platSvc.checkPropertiesLoaded();
         return platSvc;
      } catch (Exception var2) {
         this.logger.log(Level.SEVERE, "Failed to resolve BacnetMstpPlatformService", (Throwable)var2);
         return null;
      }
   }

   @Override
   public final void linkCommStart() throws Exception {
      if (this.getMstpTrunk() != 0) {
         this.getPlatformService().loadPlatformServiceProperties();
         var serialService = (BISerialService & BComponent)Sys.getService(BISerialService.TYPE);
         serialService.checkPropertiesLoaded();
         this.portComp = (BComponent)((BComponent)serialService).get(this.getPortName());
         if (this.portComp == null) {
            throw new PortNotFoundException(this.getPortName());
         } else {
            Property osPortNameProp = this.portComp.getProperty("osPortName");
            String osName = this.portComp.getString(osPortNameProp);
            BBacnetMstpPlatformService platSvc = this.getPlatformService();
            if (platSvc != null) {
               this.handle = platSvc.startDriver(
                  "/dev/mstp" + this.getMstpTrunk(),
                  osName,
                  this.getBaudRate().getOrdinal(),
                  this.getMstpAddress(),
                  this.getMaxMaster(),
                  this.getMaxInfoFrames(),
                  this
               );
               if (this.logger.isLoggable(Level.FINE)) {
                  this.logger.fine("BACnet/MSTP Driver started on port " + this.getPortName());
               }

               platSvc.setParameter(this.handle, "usageTimeout", String.valueOf(this.getUsageTimeout().getOrdinal()));
               platSvc.setParameter(this.handle, "txThrottle", String.valueOf(this.getTxThrottle()));
               this.getNetworkPort().ok();
               this.commStarted = true;
            } else {
               this.getNetworkPort().fault("Cannot find MSTP platform service");
            }
         }
      }
   }

   @Override
   public final void linkCommStop() throws Exception {
      if (this.commStarted) {
         this.commStarted = false;

         try {
            this.getPlatformService().stopDriver(this.handle);
         } finally {
            if (this.portComp != null) {
               this.portComp = null;
            }

            this.handle = -1;
         }
      }
   }

   @Override
   public byte[] getMacAddress() {
      return new byte[]{(byte)this.getMstpAddress()};
   }

   @Override
   public int getMaxAPDULengthAccepted() {
      return this.getSupportExtendedFrames() ? 1476 : 480;
   }

   @Override
   public synchronized void sendRequest(byte[] destAddress, NetworkPdu npdu) {
      byte dst;
      if (destAddress != null && destAddress.length != 0) {
         dst = destAddress[0];
      } else {
         dst = -1;
      }

      this.os.reset();
      npdu.writeNetworkBytes(this.os);
      byte[] outBuffer = this.os.toByteArray();

      try {
         if (this.commStarted) {
            if (dst != this.getMstpAddress()) {
               this.getPlatformService().sendFrame(this.handle, dst, outBuffer, npdu.getDataExpectingReply());
               if (this.logger.isLoggable(Level.FINE)) {
                  this.logger
                     .fine(
                        "MSTP Packet Sent to "
                           + dst
                           + " Size="
                           + this.os.size()
                           + " DER="
                           + npdu.getDataExpectingReply()
                           + " on "
                           + this.getPortName()
                           + ":\n"
                           + ByteArrayUtil.toHexString(outBuffer)
                     );
               }
            } else if (this.logger.isLoggable(Level.FINE)) {
               this.logger.fine("Not sending MS/TP frame to local MAC address(" + dst + ") on " + this.getPortName());
            }
         } else {
            this.logger.info("Cannot send MSTP packet! commStarted=" + this.commStarted);
         }
      } catch (Exception var6) {
         this.logger
            .log(
               Level.SEVERE,
               "Exception in BacnetMSTPLinkLayer!\n trunk=" + this.getMstpTrunk() + "\n platlet=" + this.getPlatformService() + "\n npdu=" + npdu,
               (Throwable)var6
            );
      }
   }

   public void receiveFrame(byte srcAddress, byte[] data, boolean dataExpectingReply) {
      byte[] sa = new byte[]{srcAddress};
      if (this.logger.isLoggable(Level.FINE)) {
         this.logger
            .fine(
               "MSTP Packet Received from "
                  + srcAddress
                  + " Size="
                  + data.length
                  + " DER="
                  + dataExpectingReply
                  + " on "
                  + this.getPortName()
                  + ":\n"
                  + ByteArrayUtil.toHexString(data)
            );
      }

      this.rcvIndication(sa, new byte[]{(byte)this.getMstpAddress()}, BacnetInputStream.make(data, 0, data.length));
   }

   public void spy(SpyWriter out) throws Exception {
      super.spy(out);
      out.startProps();
      out.trTitle("MSTP Link Layer", 2);
      out.prop("trunks (static)", trunks);
      if (trunks != null) {
         for (int i = 0; i < trunks.length; i++) {
            out.prop("trunks[" + i + "]", trunks[i]);
         }
      }

      out.prop("handle", this.handle);
      out.prop("commStarted", this.commStarted);
      out.prop("port", this.portComp);
      out.endProps();
   }
}
