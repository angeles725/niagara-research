package com.tridium.bacnet.stack.link.ethernet;

import com.tridium.bacnet.stack.BacnetInputStream;
import com.tridium.bacnet.stack.link.BBacnetLinkLayer;
import com.tridium.bacnet.stack.link.util.LinkLayerUtil;
import com.tridium.bacnet.stack.network.NetworkPdu;
import com.tridium.platBacnet.BBacnetEthernetPlatformService;
import com.tridium.platBacnet.BacnetEthernetAdapter;
import com.tridium.platBacnet.EthernetConst;
import com.tridium.platBacnet.EthernetListener;
import java.io.ByteArrayOutputStream;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.bacnet.BBacnetNetwork;
import javax.baja.naming.SlotPath;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.util.ByteArrayUtil;
import javax.baja.sys.BDynamicEnum;
import javax.baja.sys.BEnum;
import javax.baja.sys.BEnumRange;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.util.Lexicon;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "adapterTitle",
      type = "BEnum",
      defaultValue = "BDynamicEnum.make(-1, BEnumRange.make(noneOrdinals, noTitleArray))",
      flags = 64
   ), @NiagaraProperty(
      name = "adapterDescription",
      type = "BEnum",
      defaultValue = "BDynamicEnum.make(-1, BEnumRange.make(noneOrdinals, noDescriptionArray))",
      flags = 65
   ), @NiagaraProperty(
      name = "adapterName",
      type = "BEnum",
      defaultValue = "BDynamicEnum.make(-1, BEnumRange.make(noneOrdinals, noNameArray))",
      flags = 65
   )})
public class BBacnetEthernetLinkLayer extends BBacnetLinkLayer implements EthernetConst, EthernetListener {
   private static final Lexicon lex = Lexicon.make("bacnet");
   private static final String NONE = SlotPath.escape(lex.getText("ethernet.adapter.none"));
   private static String[] noTitleArray = new String[]{NONE};
   private static String[] noNameArray = new String[]{NONE};
   private static String[] noDescriptionArray = new String[]{NONE};
   private static int[] noneOrdinals = new int[]{-1};
   public static final Property adapterTitle = newProperty(64, BDynamicEnum.make(-1, BEnumRange.make(noneOrdinals, noTitleArray)), null);
   public static final Property adapterDescription = newProperty(65, BDynamicEnum.make(-1, BEnumRange.make(noneOrdinals, noDescriptionArray)), null);
   public static final Property adapterName = newProperty(65, BDynamicEnum.make(-1, BEnumRange.make(noneOrdinals, noNameArray)), null);
   public static final Type TYPE = Sys.loadType(BBacnetEthernetLinkLayer.class);
   private static final Logger logger = Logger.getLogger("bacnet.link.ethernet");
   private static int MAC_ADDRESS_SIZE = 6;
   private boolean commStarted = false;
   private byte[] myMacAddress;
   private String oldDeviceName = null;
   private ByteArrayOutputStream os = new ByteArrayOutputStream();
   private BacnetEthernetAdapter adapter;

   public BEnum getAdapterTitle() {
      return (BEnum)this.get(adapterTitle);
   }

   public void setAdapterTitle(BEnum v) {
      this.set(adapterTitle, v, null);
   }

   public BEnum getAdapterDescription() {
      return (BEnum)this.get(adapterDescription);
   }

   public void setAdapterDescription(BEnum v) {
      this.set(adapterDescription, v, null);
   }

   public BEnum getAdapterName() {
      return (BEnum)this.get(adapterName);
   }

   public void setAdapterName(BEnum v) {
      this.set(adapterName, v, null);
   }

   @Override
   public Type getType() {
      return TYPE;
   }

   public BBacnetEthernetPlatformService getPlatformService() {
      try {
         return (BBacnetEthernetPlatformService)Sys.getService(BBacnetEthernetPlatformService.TYPE);
      } catch (Exception var2) {
         logger.log(Level.SEVERE, "Failed to resolve BacnetEthernetPlatformService", (Throwable)var2);
         return null;
      }
   }

   @Override
   public final void linkCommInit() {
      BBacnetEthernetPlatformService platformService = this.getPlatformService();
      if (platformService != null) {
         platformService.init();
      }
   }

   @Override
   public final void linkCommStart() throws Exception {
      this.myMacAddress = new byte[MAC_ADDRESS_SIZE];
      BBacnetEthernetPlatformService platSvc = this.getPlatformService();
      if (platSvc == null) {
         logger.severe("BBacnetEthernetPlatformService unavailable, can not start link layer");
      } else {
         this.adapter = platSvc.openAdapter(SlotPath.unescape(this.getAdapterName().getTag()));
         this.adapter.addListener(this);
         this.adapter.commStart();
         this.adapter.getAddress(this.myMacAddress);
         if (logger.isLoggable(Level.FINE)) {
            logger.fine("My MAC address:" + ByteArrayUtil.toHexString(this.myMacAddress));
         }

         this.commStarted = true;
      }
   }

   @Override
   public final void linkCommStop() {
      this.adapter.removeListener(this);
      this.adapter.commStop();
      this.commStarted = false;
   }

   @Override
   public final void linkCommCleanup() {
      this.adapter.commCleanup();
      this.adapter.removeListener(this);
   }

   private void setAdapter() {
      String previousAdapterName = this.getAdapterName().getTag();
      boolean traceOn = logger.isLoggable(Level.FINE);
      if (traceOn) {
         logger.fine("Ethernet adapter original adapter name '" + previousAdapterName + "'");
      }

      Vector<String> allTitles = new Vector<>();
      Vector<String> allDescriptions = new Vector<>();
      Vector<String> allNames = new Vector<>();
      BBacnetEthernetPlatformService platformService = this.getPlatformService();
      if (platformService == null) {
         logger.warning("BBacnetEthernetPlatformService unavailable, can not set adapter");
      } else {
         try {
            platformService.getAdapterChoices(allTitles, allDescriptions, allNames);
         } catch (Exception var12) {
            logger.log(Level.WARNING, "Exception querying ethernet platform service for adapter choices", (Throwable)var12);
         }

         if (traceOn) {
            logger.fine("Ethernet Adapter Title list size:" + allTitles.size());
         }

         BEnumRange titles = LinkLayerUtil.makeEnumRange(allTitles, true, NONE);
         BEnumRange descriptions = LinkLayerUtil.makeEnumRange(allDescriptions, true, NONE);
         BEnumRange names = LinkLayerUtil.makeEnumRange(allNames, false, NONE);
         if (traceOn) {
            logger.fine("Current ethernet adapter lists:");

            for (int i = 0; i < titles.getOrdinals().length; i++) {
               logger.fine(i + ": title=" + titles + " desc=" + descriptions + " name=" + names);
            }
         }

         int ordinal = LinkLayerUtil.ordinal(previousAdapterName, names, NONE);
         String currentAdapterName = LinkLayerUtil.select(ordinal, names).getTag();
         if (!currentAdapterName.equals(previousAdapterName)) {
            logger.warning("Previous ethernet adapter '" + previousAdapterName + "' unavailable, configuration reverted to '" + currentAdapterName + "'");
         }

         this.setAdapterTitle(LinkLayerUtil.select(ordinal, titles));
         this.setAdapterDescription(LinkLayerUtil.select(ordinal, descriptions));
         this.setAdapterName(LinkLayerUtil.select(ordinal, names));
      }
   }

   public void changed(Property p, Context cx) {
      super.changed(p, cx);
      if (this.isRunning()) {
         if (p == adapterTitle) {
            this.setAdapterDescription(this.getAdapterDescription().getRange().get(this.getAdapterTitle().getOrdinal()));
            this.setAdapterName(this.getAdapterName().getRange().get(this.getAdapterTitle().getOrdinal()));
         } else if (p == adapterName) {
            String newDeviceName = this.getAdapterName().getTag();
            if (!newDeviceName.equals(this.oldDeviceName)
               && this.commStarted
               && !this.getAdapterTitle().getTag().equals(this.getLexicon().getText("ethernet.adapterTitle.none"))) {
               this.getNetworkPort().disable();
               this.getNetworkPort().enable();
            }

            this.oldDeviceName = newDeviceName;
         }
      }
   }

   public void started() {
      this.oldDeviceName = this.getAdapterName().getTag();
      BBacnetNetwork.bacnet().postAsync(new Runnable() {
         @Override
         public void run() {
            BBacnetEthernetLinkLayer.this.setAdapter();
         }
      });
   }

   public void receivePacket(byte[] packet, int bytesRcvd) {
      if (logger.isLoggable(Level.FINE)) {
         logger.fine("\nPacket Received (" + bytesRcvd + " bytes):");
         ByteArrayUtil.hexDump(packet, 0, bytesRcvd);
      }

      BacnetInputStream is = BacnetInputStream.make(packet, 0, bytesRcvd);
      if (is.skip(MAC_ADDRESS_SIZE) != MAC_ADDRESS_SIZE) {
         logger.log(Level.SEVERE, "Failed to skip destination address in read!");
      } else {
         byte[] sourceAddress = new byte[MAC_ADDRESS_SIZE];
         if (is.read(sourceAddress, 0, MAC_ADDRESS_SIZE) != MAC_ADDRESS_SIZE) {
            logger.log(Level.SEVERE, "Failed to read source address in read!");
         } else {
            int llcLength = is.read();
            llcLength <<= 8;
            llcLength |= is.read();
            if (is.available() < llcLength) {
               logger.log(Level.SEVERE, "LLC Length does not match!" + is.available() + " != " + llcLength + " packetLength: " + bytesRcvd);
            } else {
               int dsap = is.read();
               int ssap = is.read();
               int llcControl = is.read();
               if (dsap == 130 && ssap == 130 && llcControl == 3) {
                  this.rcvIndication(sourceAddress, this.myMacAddress, is);
               } else {
                  logger.log(Level.INFO, "Invalid packet received!  DSAP=" + dsap + ", SSAP=" + ssap + ", llcCtl=" + llcControl);
                  ByteArrayUtil.hexDump(packet);
               }
            }
         }
      }
   }

   @Override
   public byte[] getMacAddress() {
      return this.myMacAddress;
   }

   @Override
   public int getMaxAPDULengthAccepted() {
      return 1476;
   }

   @Override
   public synchronized void sendRequest(byte[] destAddress, NetworkPdu npdu) {
      this.os.reset();
      if (destAddress != null && destAddress.length != 0) {
         this.os.write(destAddress, 0, destAddress.length);
      } else {
         this.os.write(BCAST_MAC_ADDRESS, 0, BCAST_MAC_ADDRESS.length);
      }

      this.os.write(this.myMacAddress, 0, this.myMacAddress.length);
      this.os.write(0);
      this.os.write(0);
      this.os.write(130);
      this.os.write(130);
      this.os.write(3);
      npdu.writeNetworkBytes(this.os);
      byte[] outBuffer = this.os.toByteArray();
      int llcLength = outBuffer.length - 14;
      outBuffer[12] = (byte)((llcLength & 0xFF00) >> 8);
      outBuffer[13] = (byte)(llcLength & 0xFF);

      try {
         this.adapter.sendPacket(outBuffer);
         if (logger.isLoggable(Level.FINE)) {
            logger.fine("\nPacket Sent:");
            ByteArrayUtil.hexDump(outBuffer);
         }
      } catch (Exception var6) {
         this.getNetworkPort().fault("Cannot send Bacnet/Ethernet packet! Is the adapter enabled in the TCP/IP Platform Service?");
         logger.log(Level.SEVERE, "Cannot send Bacnet/Ethernet packet!", (Throwable)var6);
      }
   }
}
