package com.tridium.bacnet.stack.link.ip;

import com.tridium.bacnet.enums.BIpDeviceType;
import com.tridium.bacnet.stack.BacnetInputStream;
import com.tridium.bacnet.stack.link.BBacnetLinkLayer;
import com.tridium.bacnet.stack.link.ip.util.BacnetIpAdapter;
import com.tridium.bacnet.stack.link.ip.util.BacnetIpLinkUtil;
import com.tridium.bacnet.stack.link.ip.util.NetworkInterfaceProvider;
import com.tridium.bacnet.stack.link.util.LinkLayerUtil;
import com.tridium.bacnet.stack.network.NetworkPdu;
import com.tridium.nre.platform.PlatformUtil;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.BindException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.bacnet.BBacnetNetwork;
import javax.baja.bacnet.BacnetConst;
import javax.baja.naming.SlotPath;
import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraActions;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.util.Array;
import javax.baja.nre.util.ByteArrayUtil;
import javax.baja.nre.util.IntHashMap;
import javax.baja.nre.util.IntHashMap.Iterator;
import javax.baja.spy.SpyWriter;
import javax.baja.sys.Action;
import javax.baja.sys.ActionInvokeException;
import javax.baja.sys.BAbsTime;
import javax.baja.sys.BBoolean;
import javax.baja.sys.BDynamicEnum;
import javax.baja.sys.BEnum;
import javax.baja.sys.BEnumRange;
import javax.baja.sys.BFacets;
import javax.baja.sys.BInteger;
import javax.baja.sys.BRelTime;
import javax.baja.sys.BString;
import javax.baja.sys.Clock;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Slot;
import javax.baja.sys.SlotCursor;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.sys.Clock.Ticket;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "adapter",
      type = "BEnum",
      defaultValue = "BDynamicEnum.make(-1, BEnumRange.make(noneOrdinals, noAdapterArray))",
      flags = 64,
      facets = {@Facet(
         name = "BFacets.FIELD_EDITOR",
         value = "\"workbench:FrozenEnumFE\""
      ), @Facet(
         name = "BFacets.UX_FIELD_EDITOR",
         value = "\"webEditors:FrozenEnumEditor\""
      )}
   ), @NiagaraProperty(
      name = "adapterId",
      type = "BEnum",
      defaultValue = "BDynamicEnum.make(-1, BEnumRange.make(noneOrdinals, noAdapterIdArray))",
      flags = 69
   ), @NiagaraProperty(
      name = "ipAddress",
      type = "BEnum",
      defaultValue = "BDynamicEnum.make(-1, BEnumRange.make(noneOrdinals, noAdapterIpArray))",
      flags = 65
   ), @NiagaraProperty(
      name = "udpPort",
      type = "String",
      defaultValue = "BBacnetIpLinkLayer.UDP_PORT_DEFAULT"
   ), @NiagaraProperty(
      name = "ipDeviceType",
      type = "BIpDeviceType",
      defaultValue = "BIpDeviceType.standard"
   ), @NiagaraProperty(
      name = "bbmdAddress",
      type = "String",
      defaultValue = "BBacnetIpLinkLayer.BBMD_ADDRESS_DEFAULT"
   ), @NiagaraProperty(
      name = "registrationLifetime",
      type = "BRelTime",
      defaultValue = "BRelTime.make(15 * BRelTime.MILLIS_IN_MINUTE)"
   ), @NiagaraProperty(
      name = "broadcastDistributionTable",
      type = "BBroadcastDistributionTable",
      defaultValue = "new BBroadcastDistributionTable()"
   ), @NiagaraProperty(
      name = "foreignDeviceTable",
      type = "BForeignDeviceTable",
      defaultValue = "new BForeignDeviceTable()"
   ), @NiagaraProperty(
      name = "adapterDebug",
      type = "boolean",
      defaultValue = "false",
      flags = 4
   ), @NiagaraProperty(
      name = "bbmdDebug",
      type = "boolean",
      defaultValue = "false"
   ), @NiagaraProperty(
      name = "autoPollEnabled",
      type = "boolean",
      defaultValue = "false",
      flags = 5
   ), @NiagaraProperty(
      name = "adapterPollInterval",
      type = "BRelTime",
      defaultValue = "BRelTime.makeSeconds(30)",
      flags = 4,
      facets = {@Facet(
         name = "BFacets.MIN",
         value = "BRelTime.make(10)"
      )}
   )})
@NiagaraActions({@NiagaraAction(
      name = "dump"
   ), @NiagaraAction(
      name = "readBroadcastDistributionTable",
      parameterType = "BString",
      defaultValue = "BString.make(\"Enter BBMD B/IP Address\")"
   ), @NiagaraAction(
      name = "writeBroadcastDistributionTable",
      parameterType = "BString",
      defaultValue = "BString.make(\"Enter BBMD B/IP Address\")"
   ), @NiagaraAction(
      name = "updateAllBDTs",
      flags = 20
   ), @NiagaraAction(
      name = "queryForAdapters",
      flags = 128
   )})
public class BBacnetIpLinkLayer extends BBacnetLinkLayer implements Runnable, BvllConst {
   private static String NONE = lex.getText("ip.adapter.none");
   private static String[] noAdapterArray = new String[]{NONE};
   private static String[] noAdapterIdArray = new String[]{NONE};
   private static String[] noAdapterIpArray = new String[]{NONE};
   private static int[] noneOrdinals = new int[]{-1};
   private static String DISABLED = lex.getText("ip.adapter.disabled");
   private static String NO_ADDRESS = lex.getText("ip.adapter.noAddress");
   public static final Property adapter = newProperty(
      64,
      BDynamicEnum.make(-1, BEnumRange.make(noneOrdinals, noAdapterArray)),
      BFacets.make(BFacets.make("fieldEditor", "workbench:FrozenEnumFE"), BFacets.make("uxFieldEditor", "webEditors:FrozenEnumEditor"))
   );
   public static final Property adapterId = newProperty(69, BDynamicEnum.make(-1, BEnumRange.make(noneOrdinals, noAdapterIdArray)), null);
   public static final Property ipAddress = newProperty(65, BDynamicEnum.make(-1, BEnumRange.make(noneOrdinals, noAdapterIpArray)), null);
   public static final Property udpPort = newProperty(0, "0xBAC0", null);
   public static final Property ipDeviceType = newProperty(0, BIpDeviceType.standard, null);
   public static final Property bbmdAddress = newProperty(0, "null", null);
   public static final Property registrationLifetime = newProperty(0, BRelTime.make(900000L), null);
   public static final Property broadcastDistributionTable = newProperty(0, new BBroadcastDistributionTable(), null);
   public static final Property foreignDeviceTable = newProperty(0, new BForeignDeviceTable(), null);
   public static final Property adapterDebug = newProperty(4, false, null);
   public static final Property bbmdDebug = newProperty(0, false, null);
   public static final Property autoPollEnabled = newProperty(5, false, null);
   public static final Property adapterPollInterval = newProperty(4, BRelTime.makeSeconds(30), BFacets.make("min", BRelTime.make(10L)));
   public static final Action dump = newAction(0, null);
   public static final Action readBroadcastDistributionTable = newAction(0, BString.make("Enter BBMD B/IP Address"), null);
   public static final Action writeBroadcastDistributionTable = newAction(0, BString.make("Enter BBMD B/IP Address"), null);
   public static final Action updateAllBDTs = newAction(20, null);
   public static final Action queryForAdapters = newAction(128, null);
   public static final Type TYPE = Sys.loadType(BBacnetIpLinkLayer.class);
   public static final int PACKET_LENGTH = 1500;
   public static final int MAX_APDU_LENGTH = 1476;
   public static final byte[] DEFAULT_SUBNET_MASK = new byte[]{-1, -1, -1, 0};
   public static final byte[] TWO_HOP_DIST_MASK = new byte[]{-1, -1, -1, -1};
   private static final boolean LOCALHOST_ADAPTER_ALLOWED = Boolean.getBoolean("niagara.bacnet.link.ip.localhost.allow");
   private static final Logger logger = Logger.getLogger("bacnet.link.ip");
   private static int IPV4_HOST_OCTETS = 4;
   private static final String UDP_PORT_DEFAULT = "0xBAC0";
   private static final String BBMD_ADDRESS_DEFAULT = "null";
   private static final int NOT_FOUND = -1;
   private Object PORT_LOCK = new Object();
   private DatagramSocket server;
   private DatagramSocket broadcastServer = null;
   private BBacnetIpLinkLayer.BroadcastWorker broadcastWorker = null;
   private ByteArrayOutputStream os = new ByteArrayOutputStream();
   private DatagramPacket datagramOut = new DatagramPacket(new byte[0], 0);
   private String bindType = "none";
   private volatile boolean alive = false;
   private Thread myThread;
   private String oldIpAddr;
   private int myUdpPort;
   private byte[] localBroadcastAddr;
   private IntHashMap inetAddressTable = new IntHashMap();
   private Hashtable<String, byte[]> macTable = new Hashtable<>();
   private InetAddress bbmdInet = null;
   private int bbmdPort = -1;
   private byte[] bbmdMac;
   private int oldIpDeviceType = 0;
   protected byte[] myIp;
   protected byte[] myMac;
   protected volatile short netmask;
   private String[] bbmdMsgs;
   private int ndx = 0;
   private Ticket ticketAdapterPolling;

   public BEnum getAdapter() {
      return (BEnum)this.get(adapter);
   }

   public void setAdapter(BEnum v) {
      this.set(adapter, v, null);
   }

   public BEnum getAdapterId() {
      return (BEnum)this.get(adapterId);
   }

   public void setAdapterId(BEnum v) {
      this.set(adapterId, v, null);
   }

   public BEnum getIpAddress() {
      return (BEnum)this.get(ipAddress);
   }

   public void setIpAddress(BEnum v) {
      this.set(ipAddress, v, null);
   }

   public String getUdpPort() {
      return this.getString(udpPort);
   }

   public void setUdpPort(String v) {
      this.setString(udpPort, v, null);
   }

   public BIpDeviceType getIpDeviceType() {
      return (BIpDeviceType)this.get(ipDeviceType);
   }

   public void setIpDeviceType(BIpDeviceType v) {
      this.set(ipDeviceType, v, null);
   }

   public String getBbmdAddress() {
      return this.getString(bbmdAddress);
   }

   public void setBbmdAddress(String v) {
      this.setString(bbmdAddress, v, null);
   }

   public BRelTime getRegistrationLifetime() {
      return (BRelTime)this.get(registrationLifetime);
   }

   public void setRegistrationLifetime(BRelTime v) {
      this.set(registrationLifetime, v, null);
   }

   public BBroadcastDistributionTable getBroadcastDistributionTable() {
      return (BBroadcastDistributionTable)this.get(broadcastDistributionTable);
   }

   public void setBroadcastDistributionTable(BBroadcastDistributionTable v) {
      this.set(broadcastDistributionTable, v, null);
   }

   public BForeignDeviceTable getForeignDeviceTable() {
      return (BForeignDeviceTable)this.get(foreignDeviceTable);
   }

   public void setForeignDeviceTable(BForeignDeviceTable v) {
      this.set(foreignDeviceTable, v, null);
   }

   public boolean getAdapterDebug() {
      return this.getBoolean(adapterDebug);
   }

   public void setAdapterDebug(boolean v) {
      this.setBoolean(adapterDebug, v, null);
   }

   public boolean getBbmdDebug() {
      return this.getBoolean(bbmdDebug);
   }

   public void setBbmdDebug(boolean v) {
      this.setBoolean(bbmdDebug, v, null);
   }

   public boolean getAutoPollEnabled() {
      return this.getBoolean(autoPollEnabled);
   }

   public void setAutoPollEnabled(boolean v) {
      this.setBoolean(autoPollEnabled, v, null);
   }

   public BRelTime getAdapterPollInterval() {
      return (BRelTime)this.get(adapterPollInterval);
   }

   public void setAdapterPollInterval(BRelTime v) {
      this.set(adapterPollInterval, v, null);
   }

   public void dump() {
      this.invoke(dump, null, null);
   }

   public void readBroadcastDistributionTable(BString parameter) {
      this.invoke(readBroadcastDistributionTable, parameter, null);
   }

   public void writeBroadcastDistributionTable(BString parameter) {
      this.invoke(writeBroadcastDistributionTable, parameter, null);
   }

   public void updateAllBDTs() {
      this.invoke(updateAllBDTs, null, null);
   }

   public void queryForAdapters() {
      this.invoke(queryForAdapters, null, null);
   }

   @Override
   public Type getType() {
      return TYPE;
   }

   @Override
   public void linkCommInit() {
      this.updateLocalAddress();
   }

   @Override
   public void linkCommStart() throws Exception {
      this.startReception();
      if (this.isBBMDActive()) {
         this.initializeBDT();
      }
   }

   @Override
   public void linkCommStop() {
      this.stopReception();
   }

   @Override
   public void linkCommCleanup() {
      synchronized (this.PORT_LOCK) {
         this.myIp = null;
         this.myMac = null;
         if (this.server != null) {
            this.server.close();
         }

         this.server = null;
         if (this.broadcastServer != null) {
            this.broadcastServer.close();
         }

         this.broadcastServer = null;
         this.oldIpAddr = null;
         this.localBroadcastAddr = null;
      }
   }

   public void started() {
      this.oldIpDeviceType = this.getIpDeviceType().getOrdinal();
      this.oldIpAddr = this.getIpAddress().getTag();
      if (this.getBbmdDebug()) {
         this.bbmdMsgs = new String[this.getBbmdMsgsSize()];
      }

      BBacnetNetwork.bacnet().postAsync(new Runnable() {
         @Override
         public void run() {
            BBacnetIpLinkLayer.this.setTcpIpAdapter();
         }
      });
      if (this.getAutoPollEnabled()) {
         this.startAdapterPolling();
      }
   }

   public void stopped() {
      synchronized (this.PORT_LOCK) {
         this.inetAddressTable = null;
         this.macTable = null;
         this.os = null;
         this.datagramOut = null;
         this.bbmdInet = null;
         this.bbmdMac = null;
      }

      this.stopAdapterPolling();
   }

   public String toString(Context cx) {
      StringBuilder sb = new StringBuilder("B/IP (");
      sb.append(this.getIpAddress()).append(':').append(this.getUdpPort()).append(") ").append(this.getIpDeviceType());
      return sb.toString();
   }

   protected boolean processCustomBvllMessage(InetAddress srcInet, byte[] srcIp, int srcPort, int srcLength, byte[] data) {
      return false;
   }

   private void debug(String s) {
      if (this.getAdapterDebug()) {
         System.out.println("BACnet TcpIpAdapterDebug:" + s);
      }
   }

   protected void setTcpIpAdapter() {
      AccessController.doPrivileged(new BBacnetIpLinkLayer.SetTcpIpPrivilegedAction(new BBacnetIpLinkLayer.JvmNetworkInterfaceProvider()));
   }

   public void changed(Property p, Context cx) {
      super.changed(p, cx);
      if (this.isRunning()) {
         this.debug("IpLL.changed: p=" + p);
         if (p.equals(ipDeviceType)) {
            if (this.alive) {
               switch (this.oldIpDeviceType) {
                  case 1:
                     this.stopForeignDeviceRegistration();
                  case 0:
                  case 2:
                  default:
                     int bdtFlags = this.getFlags(broadcastDistributionTable);
                     int fdtFlags = this.getFlags(foreignDeviceTable);
                     boolean foreignDevice = false;
                     switch (this.getIpDeviceType().getOrdinal()) {
                        case 0:
                           this.setFlags(broadcastDistributionTable, bdtFlags | 4);
                           this.setFlags(foreignDeviceTable, fdtFlags | 4);
                           break;
                        case 1:
                           this.setFlags(broadcastDistributionTable, bdtFlags | 4);
                           this.setFlags(foreignDeviceTable, fdtFlags | 4);
                           this.startForeignDeviceRegistration();
                           foreignDevice = true;
                           break;
                        case 2:
                           this.setFlags(broadcastDistributionTable, bdtFlags & -5);
                           this.setFlags(foreignDeviceTable, fdtFlags & -5);
                           this.initializeBDT();
                     }

                     if (!foreignDevice) {
                        this.removeForeignDeviceRegistrations();
                     }
               }
            }

            this.oldIpDeviceType = this.getIpDeviceType().getOrdinal();
         } else if (p.equals(bbmdAddress)) {
            if (this.alive && this.getIpDeviceType() != BIpDeviceType.standard && !this.updateBBMDAddress(getMacBytes(this.getBbmdAddress()))) {
               logger.info("Invalid BBMD address configuration!");
            }
         } else if (p.equals(udpPort)) {
            synchronized (this) {
               if (this.alive) {
                  this.getNetworkPort().disable();
                  this.updateLocalAddress();
                  this.getNetworkPort().enable();
                  if (this.isBBMDActive()) {
                     this.checkBDT();
                  }
               } else {
                  this.updateLocalAddress();
               }
            }
         } else if (p.equals(adapter)) {
            this.setAdapterId(this.getAdapterId().getRange().get(this.getAdapter().getOrdinal()));
            this.setIpAddress(this.getIpAddress().getRange().get(this.getAdapter().getOrdinal()));
         } else if (p.equals(ipAddress)) {
            String newIpAddr = this.getIpAddress().getTag();
            if (!newIpAddr.equals(this.oldIpAddr)) {
               synchronized (this) {
                  if (this.getNetworkPort().getEnabled()) {
                     this.getNetworkPort().disable();
                     this.updateLocalAddress();
                     this.getNetworkPort().enable();
                     if (this.isBBMDActive()) {
                        this.checkBDT();
                     }
                  } else {
                     this.updateLocalAddress();
                  }
               }
            }

            this.oldIpAddr = newIpAddr;
         } else if (p.equals(bbmdDebug) || p.getName().equals("bbmdMsgsSize")) {
            this.ndx = 0;
            if (this.getBbmdDebug()) {
               this.bbmdMsgs = new String[this.getBbmdMsgsSize()];
            }
         } else if (p.equals(autoPollEnabled)) {
            if (this.getAutoPollEnabled()) {
               this.startAdapterPolling();
            } else {
               this.stopAdapterPolling();
            }
         }
      }
   }

   @Override
   public void doDump() {
      System.out.println("MAC Address Table (Inet->MAC):");
      Enumeration<String> k = this.macTable.keys();

      while (k.hasMoreElements()) {
         String inet = k.nextElement();
         byte[] mac = this.macTable.get(inet);
         System.out.println("  inet:" + inet + "\tmac:" + ByteArrayUtil.toHexString(mac));
      }

      System.out.println("Inet Address Table (MAC->Inet):");
      Iterator i = this.inetAddressTable.iterator();

      while (i.hasNext()) {
         InetAddress inet = (InetAddress)i.next();
         System.out.println("  mac:" + Integer.toHexString(i.key()) + "\tinet:" + inet);
      }

      System.out.println("local broadcast addr:" + ByteArrayUtil.toHexString(this.localBroadcastAddr));
      System.out.println("myIp:" + ByteArrayUtil.toHexString(this.myIp));
      System.out.println("oldIpAddr:" + this.oldIpAddr);
      System.out.println("myMac:" + ByteArrayUtil.toHexString(this.myMac));
      System.out.println("myUdpPort:" + this.myUdpPort);
      System.out.println("bbmdInet:" + this.bbmdInet);
      System.out.println("bbmdPort:" + this.bbmdPort);
      System.out.println("bbmdMac:" + ByteArrayUtil.toHexString(this.bbmdMac));
      System.out.println("oldIpDeviceType:" + this.oldIpDeviceType);
   }

   public void doReadBroadcastDistributionTable(BString bbmdAddress) {
      if (this.getBbmdDebug()) {
         this.trace("doReadBDT:" + bbmdAddress.getString());
      }

      try {
         StringTokenizer st = new StringTokenizer(bbmdAddress.getString(), ":");
         InetAddress inet = InetAddress.getByName(st.nextToken());
         this.sendBvllMessage(inet, Integer.decode(st.nextToken()), new ReadBroadcastDistributionTable());
         this.checkBDT();
      } catch (UnknownHostException var4) {
         logger.log(Level.SEVERE, "UnknownHostException in doReadBroadcastDistributionTable", (Throwable)var4);
         throw new IllegalArgumentException(bbmdAddress.getString());
      }
   }

   public void doWriteBroadcastDistributionTable(BString bbmdAddress) {
      if (this.getBbmdDebug()) {
         this.trace("doWriteBDT:" + bbmdAddress.getString());
      }

      try {
         StringTokenizer st = new StringTokenizer(bbmdAddress.getString(), ":");
         InetAddress inet = InetAddress.getByName(st.nextToken());
         this.sendBvllMessage(inet, Integer.decode(st.nextToken()), new WriteBroadcastDistributionTable(this.readBDT()));
      } catch (UnknownHostException var4) {
         logger.log(Level.SEVERE, "UnknownHostException in doWriteBroadcastDistributionTable", (Throwable)var4);
         throw new IllegalArgumentException(bbmdAddress.getString());
      }
   }

   public void doUpdateAllBDTs() {
   }

   public void doQueryForAdapters() {
      this.debug("Querying IP Adapter choices...");
      this.setTcpIpAdapter();
      this.debug("Finished querying IP Adapter choices!");
   }

   @Override
   public final void run() {
      this.alive = true;
      synchronized (this.PORT_LOCK) {
         if (this.startBroadcastWorker()) {
            if (this.broadcastWorker != null) {
               this.broadcastWorker.interrupt();
               this.broadcastWorker = null;
            }

            this.broadcastWorker = new BBacnetIpLinkLayer.BroadcastWorker(Thread.currentThread().getName());
            this.broadcastWorker.start();
         }
      }

      this.listenForPackets(this.server);
   }

   protected boolean startBroadcastWorker() {
      return AccessController.doPrivileged((PrivilegedAction<Boolean>)(() -> PlatformUtil.getPlatformProvider().usesPosixSockets()))
         ? !this.isLocalhost()
         : false;
   }

   protected boolean isLocalhost() {
      boolean local = false;
      if (this.myIp != null && this.myIp.length >= IPV4_HOST_OCTETS) {
         local = this.myIp[0] == 127 && this.myIp[1] == 0 && this.myIp[2] == 0 && this.myIp[3] == 1;
      }

      return local;
   }

   private void listenForPackets(DatagramSocket server) {
      while (this.alive) {
         try {
            byte[] packBuf = new byte[1500];
            DatagramPacket packIn = new DatagramPacket(packBuf, 1500);
            server.receive(packIn);
            this.processPacket(packIn, server == this.broadcastServer);
         } catch (NullPointerException var4) {
            if (server == null) {
               logger.severe("DatagramSocket is null!  Socket " + this.getPort() + " may be in use by another process...");
            }
            break;
         } catch (SocketException var5) {
            if (this.alive) {
               logger.log(Level.SEVERE, "SocketException in BBacnetIpLinkLayer!", (Throwable)var5);
               if (!this.recoverSocket()) {
                  logger.severe("Unable to recover socket! Exiting...");
                  break;
               }
            }
         } catch (IOException var6) {
            logger.log(Level.SEVERE, "Error receiving Bacnet/IP packet!", (Throwable)var6);
         } catch (Throwable var7) {
            logger.log(Level.INFO, "Unknown exception in IP Link Layer:", var7);
         }
      }
   }

   @Override
   public byte[] getMacAddress() {
      return this.myMac;
   }

   @Override
   public int getMaxAPDULengthAccepted() {
      return 1476;
   }

   @Override
   public void sendRequest(byte[] destAddress, NetworkPdu npdu) {
      if (destAddress != null && destAddress.length != 0) {
         this.sendBvllMessage(destAddress, new OriginalUnicastNpdu(npdu));
      } else {
         this.sendBvllMessage(this.localBroadcastAddr, new OriginalBroadcastNpdu(npdu));
         if (this.isBBMDActive()) {
            this.distributeBroadcastToNetwork(npdu);
         } else if (this.isForeignDevice()) {
            this.sendBroadcastToBBMD(npdu);
         }
      }
   }

   private int getPort() {
      return this.myUdpPort;
   }

   protected boolean isForeignDevice() {
      return this.getIpDeviceType() == BIpDeviceType.foreignDevice;
   }

   protected boolean isBBMDActive() {
      return this.getIpDeviceType() == BIpDeviceType.bbmd;
   }

   private void updateLocalAddress() {
      AccessController.doPrivileged(new BBacnetIpLinkLayer.UpdateLocalAddressesPrivilegedAction());
   }

   private void startReception() throws Exception {
      try {
         InetAddress myInet = this.lookupInetAddr(this.myIp);
         if (logger.isLoggable(Level.FINE)) {
            logger.fine("Start BACnet/IP reception on port " + this.getPort() + ", bound to " + myInet);
         }

         if (myInet == null && (this.getNetworkPort().getEnabled() || this.getAutoPollEnabled())) {
            this.setAutoPollEnabled(true);
            this.getNetworkPort().doDisable();
            throw new IllegalStateException("BACnet/IP adapter not ready! Check cable connections.");
         }

         if (this.startBroadcastWorker()) {
            this.server = new DatagramSocket(this.getPort(), myInet);
            this.broadcastServer = new DatagramSocket(this.getPort(), this.convertMacToAddress(this.localBroadcastAddr));
         } else {
            this.server = new DatagramSocket(this.getPort(), myInet);
         }

         this.bindType = "specific adapter";
      } catch (UnknownHostException var3) {
         logger.log(Level.SEVERE, "Unknown host:" + ByteArrayUtil.toHexString(this.myIp), (Throwable)var3);
         this.getNetworkPort().fault(lex.getText("ip.unknownHost") + ByteArrayUtil.toHexString(this.myIp));
         throw var3;
      } catch (BindException var4) {
         logger.log(Level.SEVERE, "Cannot bind datagram socket on port " + this.getPort(), (Throwable)var4);
         String msg = MessageFormat.format(lex.getText("ip.cannotBind"), this.myUdpPort);
         this.getNetworkPort().fault(msg);
         throw var4;
      } catch (SocketException var5) {
         logger.log(Level.SEVERE, "Cannot open datagram socket!", (Throwable)var5);
         this.getNetworkPort().fault(lex.getText("ip.cannotOpen"));
         throw var5;
      }

      this.myThread = new Thread(this, "BnIpLRcv");
      this.myThread.start();
      if (this.isForeignDevice()) {
         this.startForeignDeviceRegistration();
      }
   }

   private void stopReception() {
      if (this.alive) {
         logger.fine("Stop Reception");
         if (this.isForeignDevice()) {
            this.stopForeignDeviceRegistration();
         }

         synchronized (this.PORT_LOCK) {
            if (this.alive) {
               this.alive = false;
               if (this.myThread != null) {
                  this.myThread.interrupt();
               }

               this.myThread = null;
               if (this.broadcastWorker != null) {
                  this.broadcastWorker.interrupt();
               }

               this.bindType = "none";
               if (this.server != null) {
                  this.server.close();
               }

               if (this.broadcastServer != null) {
                  this.broadcastServer.close();
               }
            }
         }
      }
   }

   private void startAdapterPolling() {
      this.stopAdapterPolling();
      logger.fine("Polling for availability of the selected adapter begins.");
      this.ticketAdapterPolling = Clock.schedulePeriodically(this, BAbsTime.now(), this.getAdapterPollInterval(), queryForAdapters, null);
   }

   private void stopAdapterPolling() {
      logger.fine("Polling for availability of the selected adapter stops.");
      if (this.ticketAdapterPolling != null) {
         this.ticketAdapterPolling.cancel();
         this.ticketAdapterPolling = null;
      }
   }

   private synchronized boolean recoverSocket() {
      try {
         this.stopReception();
         logger.fine("Re-starting socket reception.");
         this.startReception();
         return true;
      } catch (Exception var2) {
         return false;
      }
   }

   private void processPacket(DatagramPacket packet, boolean isBroadcast) {
      InetAddress srcInet = packet.getAddress();
      byte[] srcIp = srcInet.getAddress();
      int srcPort = packet.getPort();
      int srcLength = packet.getLength();
      if (!ByteArrayUtil.equals(srcIp, this.myIp) || srcPort != this.myUdpPort) {
         byte[] data = new byte[srcLength];
         byte[] pktData = packet.getData();
         System.arraycopy(pktData, 0, data, 0, srcLength);
         if (logger.isLoggable(Level.FINE)) {
            logger.fine("\nPacket Received(" + srcLength + "): addr=" + srcInet.getHostAddress() + ", port=" + srcPort);
            ByteArrayUtil.hexDump(data);
         }

         byte[] srcAddress = this.lookupMacAddress(srcInet, srcPort);
         BacnetInputStream in = BacnetInputStream.make(data, 0, data.length);
         int type = in.read();
         if (type == 129) {
            int function = in.read();
            in.read();
            in.read();
            if (this.getBbmdDebug()) {
               switch (function) {
                  case 0:
                  case 1:
                  case 2:
                  case 3:
                  case 5:
                  case 6:
                  case 7:
                  case 8:
                  case 9:
                     this.trace(
                        "\nBBMD Message Received("
                           + srcLength
                           + "): addr="
                           + srcInet.getHostAddress()
                           + ", port="
                           + srcPort
                           + ": "
                           + ByteArrayUtil.toHexString(data)
                     );
                  case 4:
               }
            }

            switch (function) {
               case 0:
                  int ms = in.read();
                  int ls = in.read();
                  int resultCode = ms << 8 | ls;
                  if (this.getBbmdDebug()) {
                     this.trace("BVLC-Result received: resultCode=" + resultCode);
                  }

                  if (logger.isLoggable(Level.FINE)) {
                     logger.fine("BVLC Result " + resultCode + " received from " + srcInet);
                  }
                  break;
               case 1:
                  if (this.isBBMDActive()) {
                     if (this.getBbmdDebug()) {
                        this.trace("Write-BDT received");
                     }

                     if (this.writeBDT(in)) {
                        this.sendBvllMessage(srcInet, srcPort, BvlcResult.OK);
                     } else {
                        this.sendBvllMessage(srcInet, srcPort, BvlcResult.WRITE_BDT_NAK);
                     }

                     if (this.getBbmdDebug()) {
                        this.trace("checkBDT() on WriteBDT rec");
                     }

                     this.checkBDT();
                  } else {
                     this.sendBvllMessage(srcInet, srcPort, BvlcResult.WRITE_BDT_NAK);
                  }
                  break;
               case 2:
                  if (this.isBBMDActive()) {
                     if (this.getBbmdDebug()) {
                        this.trace("Read-BDT received");
                     }

                     byte[] bdt = this.readBDT();
                     if (bdt == null) {
                        this.sendBvllMessage(srcInet, srcPort, BvlcResult.READ_BDT_NAK);
                     } else {
                        this.sendBvllMessage(srcInet, srcPort, new ReadBroadcastDistributionTableAck(bdt));
                     }
                  } else {
                     this.sendBvllMessage(srcInet, srcPort, BvlcResult.READ_BDT_NAK);
                  }
                  break;
               case 3:
                  if (this.isBBMDActive()) {
                     if (this.getBbmdDebug()) {
                        this.trace("Read-BDT-Ack received");
                     }

                     this.writeBDT(in);
                     if (this.getBbmdDebug()) {
                        this.trace("checkBDT() on ReadBDTAck rec");
                     }

                     this.checkBDT();
                  }
                  break;
               case 4:
                  byte[] fwdAddress = new byte[6];

                  for (int i = 0; i < 6; i++) {
                     fwdAddress[i] = (byte)in.read();
                  }

                  if (logger.isLoggable(Level.FINE)) {
                     logger.fine("Forwarded NPDU received from original device:" + ByteArrayUtil.toHexString(fwdAddress));
                  }

                  if (this.isBBMDActive()) {
                     in.mark(in.getPos());
                     this.distributeBroadcastFromNetwork(in, fwdAddress, srcAddress);
                     in.reset();
                  }

                  this.rcvIndication(fwdAddress, this.myMac, in);
                  break;
               case 5:
                  if (this.isBBMDActive()) {
                     if (this.getBbmdDebug()) {
                        this.trace("Register-FD received");
                     }

                     this.sendBvllMessage(srcInet, srcPort, this.registerForeignDevice(in, srcAddress));
                  } else {
                     this.sendBvllMessage(srcInet, srcPort, BvlcResult.REGISTER_FD_NAK);
                  }
                  break;
               case 6:
                  if (this.isBBMDActive()) {
                     if (this.getBbmdDebug()) {
                        this.trace("Read-FDT received");
                     }

                     this.sendBvllMessage(srcInet, srcPort, this.readFDT());
                  } else {
                     this.sendBvllMessage(srcInet, srcPort, BvlcResult.READ_FDT_NAK);
                  }
                  break;
               case 7:
                  if (this.isBBMDActive()) {
                     if (this.getBbmdDebug()) {
                        this.trace("Read-FDT-Ack received");
                     }

                     this.writeFDT(in);
                  }
                  break;
               case 8:
                  if (this.isBBMDActive()) {
                     if (this.getBbmdDebug()) {
                        this.trace("Delete-FDTE received");
                     }

                     this.sendBvllMessage(srcInet, srcPort, this.unregisterForeignDevice(in));
                  } else {
                     this.sendBvllMessage(srcInet, srcPort, BvlcResult.DELETE_FDT_ENTRY_NAK);
                  }
                  break;
               case 9:
                  if (this.isBBMDActive()) {
                     if (this.getBbmdDebug()) {
                        this.trace("Distribute-Broadcast received");
                     }

                     in.mark(in.getPos());
                     if (!this.distributeBroadcastToNetwork(in, srcAddress, true)) {
                        this.sendBvllMessage(srcInet, srcPort, BvlcResult.DIST_BCAST_NAK);
                     }

                     in.reset();
                     this.rcvIndication(srcAddress, this.myMac, in);
                  } else {
                     this.sendBvllMessage(srcInet, srcPort, BvlcResult.DIST_BCAST_NAK);
                  }
                  break;
               case 10:
                  this.rcvIndication(srcAddress, this.myMac, in, isBroadcast);
                  break;
               case 11:
                  if (this.isBBMDActive()) {
                     in.mark(in.getPos());
                     this.distributeBroadcastToNetwork(in, srcAddress, false);
                     in.reset();
                  }

                  this.rcvIndication(srcAddress, this.myMac, in, true);
                  break;
               default:
                  if (!this.processCustomBvllMessage(srcInet, srcIp, srcPort, srcLength, data)) {
                     logger.info("Invalid BVLL function: " + function);
                  }
            }
         }
      }
   }

   private boolean writeBDT(BacnetInputStream in) {
      boolean dbg = this.getBbmdDebug();
      if (dbg) {
         this.trace("writeBDT");
      }

      int avail = in.available();
      if (avail % 10 != 0) {
         logger.log(Level.SEVERE, "Invalid BVLL BDT size!");
         return false;
      } else {
         try {
            BBroadcastDistributionTable table = this.getBroadcastDistributionTable();
            Array<BBdtEntry> newTable = new Array(BBdtEntry.class);
            int len = avail / 10;

            for (int i = 0; i < len; i++) {
               byte[] bbmdAddr = new byte[6];
               in.read(bbmdAddr);
               byte[] bbmdBDMask = new byte[IPV4_HOST_OCTETS];
               in.read(bbmdBDMask);
               BBdtEntry e = new BBdtEntry(bbmdAddr, bbmdBDMask);
               if (dbg) {
                  this.trace("Adding BDT Entry: " + e);
               }

               newTable.add(e);
            }

            boolean forceBDTWrite = table.updateBDT((BBdtEntry[])newTable.trim());
            if (forceBDTWrite) {
               this.checkBDT(true);
            }

            return true;
         } catch (Exception var11) {
            logger.log(Level.SEVERE, "Unable to write BDT data!", (Throwable)var11);
            return false;
         }
      }
   }

   private byte[] readBDT() {
      boolean dbg = this.getBbmdDebug();
      if (dbg) {
         this.trace("readBDT");
      }

      try {
         BBroadcastDistributionTable table = this.getBroadcastDistributionTable();
         SlotCursor<Property> c = table.getProperties();
         ByteArrayOutputStream out = new ByteArrayOutputStream();

         while (c.next(BBdtEntry.class)) {
            BBdtEntry e = (BBdtEntry)c.get();
            if (dbg) {
               this.trace("Encoding BDTEntry:" + e);
            }

            out.write(e.getBIpAddr());
            out.write(e.getBdMask());
         }

         return out.toByteArray();
      } catch (IOException var6) {
         logger.log(Level.SEVERE, "IOException reading BDT!", (Throwable)var6);
         return null;
      }
   }

   private BvllMessage registerForeignDevice(BacnetInputStream in, byte[] srcAddress) {
      int msb = in.read();
      int lsb = in.read();
      int timeToLive = msb << 8 | lsb;
      if (this.getBbmdDebug()) {
         this.trace("registerFD: Addr=" + ByteArrayUtil.toHexString(srcAddress) + "; TTL=" + timeToLive);
      }

      this.getForeignDeviceTable().addEntry(srcAddress, timeToLive);
      return BvlcResult.OK;
   }

   private BvllMessage readFDT() {
      boolean dbg = this.getBbmdDebug();
      if (dbg) {
         this.trace("readFDT");
      }

      try {
         BForeignDeviceTable table = this.getForeignDeviceTable();
         SlotCursor<Property> c = table.getProperties();
         ByteArrayOutputStream out = new ByteArrayOutputStream();
         long curTime = BAbsTime.make().getMillis();

         while (c.next(BFdtEntry.class)) {
            BFdtEntry e = (BFdtEntry)c.get();
            if (dbg) {
               this.trace("Encoding FDTEntry:" + e);
            }

            out.write(e.getBIpAddr());
            int timeToLive = e.getTimeToLive();
            out.write(timeToLive >> 8 & 0xFF);
            out.write(timeToLive & 0xFF);
            int timeRemaining = (int)((e.getPurgeTime().getMillis() - curTime) / 1000L);
            out.write(timeRemaining >> 8 & 0xFF);
            out.write(timeRemaining & 0xFF);
         }

         return new ReadForeignDeviceTableAck(out.toByteArray());
      } catch (IOException var10) {
         logger.log(Level.SEVERE, "Error reading FDT!", (Throwable)var10);
         return BvlcResult.READ_FDT_NAK;
      }
   }

   private void writeFDT(BacnetInputStream in) {
      if (this.getBbmdDebug()) {
         this.trace("writeFDT");
      }
   }

   private BvllMessage unregisterForeignDevice(BacnetInputStream in) {
      byte[] fdAddr = new byte[6];
      in.read(fdAddr);
      if (this.getBbmdDebug()) {
         this.trace("unregisterFD: fdAddr=" + ByteArrayUtil.toHexString(fdAddr));
      }

      boolean ok = this.getForeignDeviceTable().deleteEntry(fdAddr);
      return ok ? BvlcResult.OK : BvlcResult.DELETE_FDT_ENTRY_NAK;
   }

   private void distributeBroadcastToNetwork(NetworkPdu npdu) {
      BvllMessage msg = new ForwardedNpdu(npdu, this.myMac);
      SlotCursor<Property> c = this.getBroadcastDistributionTable().getProperties();

      while (c.next(BBdtEntry.class)) {
         if (!c.property().getName().equals("localDevice")) {
            BBdtEntry e = (BBdtEntry)c.get();
            byte[] bdtAddr = new byte[6];
            byte[] bIPAddr = e.getBIpAddr();
            System.arraycopy(bIPAddr, 0, bdtAddr, 0, 6);
            byte[] bdtMask = e.getBdMask();

            for (int i = 0; i < 4; i++) {
               bdtAddr[i] = (byte)(bdtAddr[i] | ~bdtMask[i]);
            }

            this.sendBvllMessage(bdtAddr, msg);
         }
      }

      c = this.getForeignDeviceTable().getProperties();

      while (c.next(BFdtEntry.class)) {
         BFdtEntry e = (BFdtEntry)c.get();
         byte[] entryAddr = e.getBIpAddr();
         this.sendBvllMessage(entryAddr, msg);
      }
   }

   private boolean distributeBroadcastToNetwork(BacnetInputStream in, byte[] srcAddr, boolean foreignDevice) {
      BvllMessage msg = new ForwardedNpdu(in, srcAddr);
      if (foreignDevice) {
         if (!this.isForeignDevice(srcAddr)) {
            return false;
         }

         this.sendBvllMessage(this.localBroadcastAddr, msg);
      }

      SlotCursor<Property> c = this.getBroadcastDistributionTable().getProperties();

      while (c.next(BBdtEntry.class)) {
         BBdtEntry e = (BBdtEntry)c.get();
         byte[] bdtAddr = new byte[6];
         byte[] bIPAddr = e.getBIpAddr();
         System.arraycopy(bIPAddr, 0, bdtAddr, 0, 6);
         byte[] bdtMask = e.getBdMask();

         for (int i = 0; i < 4; i++) {
            bdtAddr[i] = (byte)(bdtAddr[i] | ~bdtMask[i]);
         }

         this.sendBvllMessage(bdtAddr, msg);
      }

      c = this.getForeignDeviceTable().getProperties();

      while (c.next(BFdtEntry.class)) {
         BFdtEntry e = (BFdtEntry)c.get();
         byte[] entryAddr = e.getBIpAddr();
         if (foreignDevice) {
            for (int i = 0; i < 4; i++) {
               if (srcAddr[i] != entryAddr[i]) {
                  this.sendBvllMessage(entryAddr, msg);
                  break;
               }
            }
         } else {
            this.sendBvllMessage(entryAddr, msg);
         }
      }

      return true;
   }

   public boolean isForeignDevice(byte[] srcAddr) {
      SlotCursor<Property> c = this.getForeignDeviceTable().getProperties();

      while (c.next(BFdtEntry.class)) {
         BFdtEntry e = (BFdtEntry)c.get();
         byte[] entryAddr = e.getBIpAddr();
         if (Arrays.equals(srcAddr, entryAddr)) {
            return true;
         }
      }

      return false;
   }

   private void distributeBroadcastFromNetwork(BacnetInputStream in, byte[] origSrcAddress, byte[] srcAddress) {
      BvllMessage msg = new ForwardedNpdu(in, origSrcAddress);
      BBdtEntry me = (BBdtEntry)this.getBroadcastDistributionTable().get("localDevice");
      if (me != null && !me.isDirectedBroadcast() && !BacnetIpLinkUtil.isSourceLocal(srcAddress, this.myMac, this.netmask)) {
         this.sendBvllMessage(this.localBroadcastAddr, msg);
      }

      SlotCursor<Property> c = this.getForeignDeviceTable().getProperties();

      while (c.next(BFdtEntry.class)) {
         BFdtEntry e = (BFdtEntry)c.get();
         byte[] entryAddr = e.getBIpAddr();
         this.sendBvllMessage(entryAddr, msg);
      }
   }

   protected void sendBroadcastToBBMD(NetworkPdu npdu) {
      SlotCursor<Slot> sc = this.getSlots();

      while (sc.next(BForeignDeviceRegistration.class)) {
         try {
            BForeignDeviceRegistration fdreg = (BForeignDeviceRegistration)sc.get();
            fdreg.sendBvll(new DistributeBroadcastToNetwork(npdu));
         } catch (Exception var4) {
            logger.log(Level.SEVERE, "Unable to send broadcast to BBMD!", (Throwable)var4);
         }
      }
   }

   protected void sendBvllMessage(byte[] destAddress, BvllMessage msg) {
      if (destAddress == null) {
         destAddress = this.localBroadcastAddr;
      }

      try {
         if (destAddress.length < 6) {
            throw new UnknownHostException("Invalid Bacnet/IP MAC address! " + ByteArrayUtil.toHexString(destAddress));
         }

         InetAddress ip = this.lookupInetAddr(destAddress);
         int port = getPort(destAddress);
         this.sendBvllMessage(ip, port, msg);
      } catch (UnknownHostException var5) {
         logger.log(Level.SEVERE, "Cannot find host for destAddress: " + ByteArrayUtil.toHexString(destAddress));
      }
   }

   protected void sendBvllMessage(InetAddress inet, int port, BvllMessage msg) {
      if (this.myIp != null && (!ByteArrayUtil.equals(this.myIp, inet.getAddress()) || port != this.myUdpPort)) {
         byte[] outBuffer = null;
         synchronized (this.PORT_LOCK) {
            try {
               this.os.reset();
               outBuffer = msg.encode(this.os);
               this.datagramOut.setAddress(inet);
               this.datagramOut.setPort(port);
               this.datagramOut.setData(outBuffer);
               this.datagramOut.setLength(outBuffer.length);
               if (this.server != null) {
                  this.server.send(this.datagramOut);
                  if (logger.isLoggable(Level.FINE)) {
                     logger.fine(
                        "\nPacket Sent on port "
                           + this.server.getLocalPort()
                           + ": ip="
                           + inet
                           + " port="
                           + port
                           + " length="
                           + (outBuffer != null ? outBuffer.length : 0)
                     );
                     ByteArrayUtil.hexDump(outBuffer);
                  }
               }
            } catch (Exception var8) {
               logger.log(Level.SEVERE, "Cannot send Bacnet/IP packet!", (Throwable)var8);
            }
         }

         if (this.getBbmdDebug()) {
            switch (msg.function) {
               case 0:
               case 1:
               case 2:
               case 3:
               case 5:
               case 6:
               case 7:
               case 8:
               case 9:
                  this.trace(
                     "Sending BBMD Message ("
                        + (outBuffer != null ? outBuffer.length : 0)
                        + "): addr="
                        + inet.getHostAddress()
                        + ", port="
                        + port
                        + ": "
                        + ByteArrayUtil.toHexString(outBuffer)
                  );
               case 4:
            }
         }
      }
   }

   public static byte[] parseIp(String ipStr) {
      if (ipStr == null) {
         return DEFAULT_SUBNET_MASK;
      } else {
         try {
            return InetAddress.getByName(ipStr).getAddress();
         } catch (UnknownHostException var2) {
            logger.log(Level.SEVERE, "UnknownHostException in parseIp", (Throwable)var2);
            throw new IllegalArgumentException();
         }
      }
   }

   protected static int getPort(byte[] destAddress) {
      int port = (destAddress[4] & 255) << 8;
      return port | destAddress[5] & 0xFF;
   }

   protected InetAddress lookupInetAddr(byte[] destAddress) throws UnknownHostException {
      this.debug("lookupInetAddr:" + ByteArrayUtil.toHexString(destAddress));
      if (destAddress == null) {
         return null;
      } else {
         int ipHash = ipHash(destAddress);
         if (this.inetAddressTable == null) {
            return null;
         } else {
            InetAddress inetAddr = (InetAddress)this.inetAddressTable.get(ipHash);
            if (inetAddr == null) {
               StringBuilder sb = new StringBuilder(15);
               sb.append(destAddress[0] & 255);
               sb.append(".");
               sb.append(destAddress[1] & 255);
               sb.append(".");
               sb.append(destAddress[2] & 255);
               sb.append(".");
               sb.append(destAddress[3] & 255);
               inetAddr = InetAddress.getByName(sb.toString());
               this.inetAddressTable.put(ipHash, inetAddr);
            }

            return inetAddr;
         }
      }
   }

   private byte[] lookupMacAddress(InetAddress inet, int port) {
      String key = inet.getHostAddress() + port;
      byte[] macAddress = this.macTable.get(key);
      if (macAddress == null) {
         macAddress = new byte[6];
         byte[] ip = inet.getAddress();
         System.arraycopy(ip, 0, macAddress, 0, IPV4_HOST_OCTETS);
         macAddress[4] = (byte)((port & 0xFF00) >> 8);
         macAddress[5] = (byte)(port & 0xFF);
         this.macTable.put(key, macAddress);
      }

      return macAddress;
   }

   private static int ipHash(byte[] ip) {
      int ipHash = ip[3] & 255;
      ipHash |= ip[2] << 8 & 0xFF00;
      ipHash |= ip[1] << 16 & 0xFF0000;
      return ipHash | ip[0] << 24 & 0xFF000000;
   }

   protected static byte[] getMacBytes(String addr) {
      if (addr != null && addr.length() != 0 && !addr.equalsIgnoreCase("null")) {
         try {
            byte[] b = new byte[6];
            int ndx = addr.indexOf(".");
            if (ndx > 0) {
               StringTokenizer st = new StringTokenizer(addr, ".:");
               if (st.countTokens() < 5) {
                  return null;
               }

               for (int i = 0; i < IPV4_HOST_OCTETS; i++) {
                  b[i] = (byte)Integer.decode(st.nextToken()).intValue();
               }

               int port = Integer.decode(st.nextToken());
               b[4] = (byte)(port >> 8 & 0xFF);
               b[5] = (byte)(port & 0xFF);
            } else {
               StringTokenizer st = new StringTokenizer(addr, " :");
               if (st.countTokens() < 6) {
                  return null;
               }

               for (int i = 0; i < 6; i++) {
                  b[i] = (byte)Integer.parseInt(st.nextToken(), 16);
               }
            }

            return b;
         } catch (Exception var5) {
            return null;
         }
      } else {
         return null;
      }
   }

   private InetAddress convertMacToAddress(byte[] mac) throws UnknownHostException {
      byte[] macAddress;
      if (mac.length == IPV4_HOST_OCTETS) {
         macAddress = mac;
      } else {
         macAddress = new byte[IPV4_HOST_OCTETS];
         System.arraycopy(mac, 0, macAddress, 0, IPV4_HOST_OCTETS);
      }

      return InetAddress.getByAddress(macAddress);
   }

   protected void startForeignDeviceRegistration() {
      logger.fine("Starting foreign device registration...");
      SlotCursor<Slot> sc = this.getSlots();
      BForeignDeviceRegistration fdReg = null;

      while (sc.next(BForeignDeviceRegistration.class)) {
         fdReg = (BForeignDeviceRegistration)sc.get();
         fdReg.registerWithBBMD();
      }

      if (fdReg == null) {
         fdReg = new BForeignDeviceRegistration(this.getBbmdAddress(), this.getRegistrationLifetime());
         fdReg.set(BForeignDeviceRegistration.enabled, BBoolean.TRUE, BacnetConst.noWrite);
         this.add("ForeignDeviceReg?", fdReg);
         if (!Sys.atSteadyState()) {
            fdReg.registerWithBBMD();
         }
      }
   }

   protected void stopForeignDeviceRegistration() {
      SlotCursor<Slot> sc = this.getSlots();
      BForeignDeviceRegistration fdReg = null;

      while (sc.next(BForeignDeviceRegistration.class)) {
         fdReg = (BForeignDeviceRegistration)sc.get();
         if (fdReg.getEnabled()) {
            fdReg.unregisterWithBBMD();
         }
      }
   }

   protected void removeForeignDeviceRegistrations() {
      BForeignDeviceRegistration[] fdregs = (BForeignDeviceRegistration[])this.getChildren(BForeignDeviceRegistration.class);

      for (int i = 0; i < fdregs.length; i++) {
         this.remove(fdregs[i]);
      }
   }

   protected boolean updateBBMDAddress(byte[] newMacAddress) {
      if (this.isBBMDActive()) {
         this.initializeBDT();
      }

      return true;
   }

   private void initializeBDT() {
      logger.fine("Initializing the Broadcast Distribution Table");
      byte[] bbmdMacAddr = getMacBytes(this.getBbmdAddress());

      try {
         if (bbmdMacAddr != null && bbmdMacAddr.length == 6) {
            this.sendBvllMessage(bbmdMacAddr, new ReadBroadcastDistributionTable());
         }

         this.checkBDT();
      } catch (Exception var3) {
         logger.log(Level.INFO, " - Unable to read BroadcastDistributionTable from BBMD", (Throwable)var3);
         if (bbmdMacAddr != null) {
            ByteArrayUtil.hexDump(bbmdMacAddr);
         }
      }
   }

   boolean checkBDT() {
      return this.checkBDT(false);
   }

   private boolean checkBDT(boolean forceWrite) {
      boolean dbg = this.getBbmdDebug();
      if (dbg) {
         this.trace("checkBDT(" + (forceWrite ? "T)" : "F)"));
      }

      boolean bdtChanged = false;
      if (this.getBroadcastDistributionTable().get("localDevice") == null) {
         logger.fine("Adding ourself to the BDT...");
         if (dbg) {
            this.trace("adding local to BDT");
         }

         this.getBroadcastDistributionTable().add("localDevice", new BBdtEntry(this.myMac, TWO_HOP_DIST_MASK), BBroadcastDistributionTable.noValidation);
         bdtChanged = true;
      } else if (!((BBdtEntry)this.getBroadcastDistributionTable().get("localDevice")).ipEquals(this.myMac)) {
         if (dbg) {
            this.trace("updating local in BDT");
         }

         this.getBroadcastDistributionTable().remove("localDevice");
         this.getBroadcastDistributionTable().add("localDevice", new BBdtEntry(this.myMac, TWO_HOP_DIST_MASK), BBroadcastDistributionTable.noValidation);
         bdtChanged = true;
      }

      if (bdtChanged || forceWrite) {
         this.updateAllBDTs();
      }

      return bdtChanged;
   }

   private int getBbmdMsgsSize() {
      int sz = 500;

      try {
         Property p = this.getProperty("bbmdMsgsSize");
         if (p != null && p.getType().is(BInteger.TYPE)) {
            sz = this.getInt(p);
         }
      } catch (Exception var3) {
      }

      return sz;
   }

   private void trace(String s) {
      System.out.println(">>>BBMD<<< " + s);
      this.bbmdMsgs[this.ndx] = Clock.time() + ":" + s;
      if (++this.ndx >= this.bbmdMsgs.length) {
         this.ndx = 0;
      }
   }

   public void spy(SpyWriter out) throws Exception {
      super.spy(out);
      out.startProps();
      out.trTitle("BacnetIpLinkLayer", 2);
      out.prop("MAC Address Table", "Inet -> MAC");
      Enumeration<String> k = this.macTable.keys();

      while (k.hasMoreElements()) {
         String adr = k.nextElement();
         byte[] m = this.macTable.get(adr);
         out.prop("  " + adr, m);
      }

      out.prop("Inet Address Table", "MAC -> Inet");
      Iterator it = this.inetAddressTable.iterator();

      while (it.hasNext()) {
         InetAddress i = (InetAddress)it.next();
         out.prop("  " + it.key(), i);
      }

      out.prop("localAddress", this.server.getLocalAddress());
      out.prop("localPort", this.server.getLocalPort());
      out.prop("bindType", this.bindType);
      out.prop("localBroadcastAddr", ByteArrayUtil.toHexString(this.localBroadcastAddr));
      out.prop("myIp", ByteArrayUtil.toHexString(this.myIp));
      out.prop("oldIpAddr", this.oldIpAddr);
      out.prop("myMac", ByteArrayUtil.toHexString(this.myMac));
      out.prop("myUdpPort", this.myUdpPort);
      out.prop("bbmdInet", this.bbmdInet);
      out.prop("bbmdPort", this.bbmdPort);
      out.prop("bbmdMac", ByteArrayUtil.toHexString(this.bbmdMac));
      out.prop("oldIpDeviceType", this.oldIpDeviceType);
      if (this.getBbmdDebug()) {
         out.prop("BBMD Messages (last " + this.bbmdMsgs.length + "): now=", Clock.time());

         for (int j = this.ndx; j < this.bbmdMsgs.length; j++) {
            if (this.bbmdMsgs[j] != null) {
               out.prop(j + ":", this.bbmdMsgs[j]);
            }
         }

         if (this.ndx != 0) {
            for (int jx = 0; jx < this.ndx; jx++) {
               if (this.bbmdMsgs[jx] != null) {
                  out.prop(jx + ":", this.bbmdMsgs[jx]);
               }
            }
         }
      }

      out.endProps();
   }

   protected byte[] getMac() {
      return this.myMac;
   }

   private class BroadcastWorker extends Thread {
      public BroadcastWorker(String id) {
         super(id + "_broadcast");
      }

      @Override
      public void run() {
         BBacnetIpLinkLayer.this.listenForPackets(BBacnetIpLinkLayer.this.broadcastServer);
      }
   }

   private class JvmNetworkInterfaceProvider implements NetworkInterfaceProvider {
      private JvmNetworkInterfaceProvider() {
      }

      @Override
      public Collection<BacnetNetworkAdapter> getInterfaces() throws SocketException {
         Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
         List<BacnetNetworkAdapter> adapters = new ArrayList<>();
         if (interfaces == null) {
            return adapters;
         } else {
            while (interfaces.hasMoreElements()) {
               NetworkInterface netIf = interfaces.nextElement();
               if (!netIf.isLoopback() || BBacnetIpLinkLayer.LOCALHOST_ADAPTER_ALLOWED) {
                  String ip = "";

                  for (InetAddress address : Collections.list(netIf.getInetAddresses())) {
                     if (address instanceof Inet4Address) {
                        ip = LinkLayerUtil.addressToString(address.getAddress());
                        break;
                     }
                  }

                  if (ip.length() == 0) {
                     if (!BBacnetIpLinkLayer.this.getAdapterId().getTag().equals(netIf.getName())) {
                        continue;
                     }

                     ip = "0.0.0.0";
                  } else if (BBacnetIpLinkLayer.this.getAdapterId().getTag().equals(netIf.getName()) && BBacnetIpLinkLayer.this.getAutoPollEnabled()) {
                     BBacnetIpLinkLayer.this.setAutoPollEnabled(false);
                     BBacnetIpLinkLayer.this.getNetworkPort().doEnable();
                  }

                  adapters.add(new BacnetIpAdapter(netIf, ip));
               }
            }

            return adapters;
         }
      }
   }

   protected class SetTcpIpPrivilegedAction implements PrivilegedAction<Void> {
      private NetworkInterfaceProvider provider;

      public SetTcpIpPrivilegedAction(NetworkInterfaceProvider provider) {
         this.provider = provider;
      }

      public Void run() {
         try {
            BBacnetIpLinkLayer.this.debug("setTcpIpAdapter()");
            if (BBacnetIpLinkLayer.this.getAdapterDebug()) {
               new Throwable().printStackTrace();
            }

            String currentTag = BBacnetIpLinkLayer.this.getAdapterId().getTag();
            int currentOrdinal = BBacnetIpLinkLayer.this.getAdapterId().getOrdinal();
            BBacnetIpLinkLayer.this.debug("currentTag=" + currentTag + ", currentOrdinal=" + currentOrdinal);
            Collection<BacnetNetworkAdapter> allAdapters = this.provider.getInterfaces();
            Collection<BacnetNetworkAdapter> filtered = LinkLayerUtil.filterAdapters(allAdapters);
            BEnumRange idRange = LinkLayerUtil.makeIdRange(filtered, BBacnetIpLinkLayer.NONE);
            int ordinal = LinkLayerUtil.ordinal(currentTag, idRange, BBacnetIpLinkLayer.NONE);
            BBacnetIpLinkLayer.this.debug("newOrdinal=" + ordinal);
            BBacnetIpLinkLayer.this.setAdapterId(LinkLayerUtil.select(ordinal, idRange));
            BBacnetIpLinkLayer.this.setIpAddress(LinkLayerUtil.select(ordinal, LinkLayerUtil.makeIpRange(filtered, BBacnetIpLinkLayer.NONE)));
            BBacnetIpLinkLayer.this.setAdapter(LinkLayerUtil.select(ordinal, LinkLayerUtil.makeDescRange(filtered, BBacnetIpLinkLayer.NONE)));
         } catch (ActionInvokeException var7) {
            BBacnetIpLinkLayer.logger.log(Level.SEVERE, "ActionInvokeException in setTcpIpAdapter", (Throwable)var7);
            throw var7;
         } catch (SocketException var8) {
            BBacnetIpLinkLayer.logger.log(Level.SEVERE, "SocketException in setTcpIpAdapter", (Throwable)var8);
         }

         return null;
      }
   }

   private class UpdateLocalAddressesPrivilegedAction implements PrivilegedAction<Void> {
      private UpdateLocalAddressesPrivilegedAction() {
      }

      public Void run() {
         try {
            Enumeration<NetworkInterface> interfaceEnumeration = NetworkInterface.getNetworkInterfaces();
            if (interfaceEnumeration == null) {
               return null;
            }

            List<NetworkInterface> ifList = Collections.list(interfaceEnumeration);
            NetworkInterface adapter = null;

            for (int i = 0; i < ifList.size(); i++) {
               NetworkInterface netIf = ifList.get(i);
               if (netIf.getName().equalsIgnoreCase(SlotPath.unescape(BBacnetIpLinkLayer.this.getAdapterId().getTag()))) {
                  adapter = netIf;
                  break;
               }
            }

            if (adapter == null) {
               return null;
            }

            if (!adapter.getInetAddresses().hasMoreElements()) {
               return null;
            }

            try {
               int myPort = Integer.decode(BBacnetIpLinkLayer.this.getUdpPort());
               BBacnetIpLinkLayer.this.myUdpPort = myPort;
            } catch (NumberFormatException var7) {
               BBacnetIpLinkLayer.logger.log(Level.SEVERE, "Invalid BACnet/IP UDP Port:" + BBacnetIpLinkLayer.this.getUdpPort());
            }

            BBacnetIpLinkLayer.this.localBroadcastAddr = new byte[6];

            for (InterfaceAddress iAddr : adapter.getInterfaceAddresses()) {
               if (iAddr.getAddress().getHostAddress().equalsIgnoreCase(SlotPath.unescape(BBacnetIpLinkLayer.this.getIpAddress().getTag()))) {
                  BBacnetIpLinkLayer.this.netmask = iAddr.getNetworkPrefixLength();
                  byte[] bcast = BacnetIpLinkUtil.getBroadcastAddress(iAddr.getAddress().getAddress(), BBacnetIpLinkLayer.this.netmask);
                  System.arraycopy(bcast, 0, BBacnetIpLinkLayer.this.localBroadcastAddr, 0, 4);
               }
            }

            BBacnetIpLinkLayer.this.myMac = new byte[6];
            BBacnetIpLinkLayer.this.myIp = BBacnetIpLinkLayer.parseIp(SlotPath.unescape(BBacnetIpLinkLayer.this.getIpAddress().getTag()));
            System.arraycopy(BBacnetIpLinkLayer.this.myIp, 0, BBacnetIpLinkLayer.this.myMac, 0, BBacnetIpLinkLayer.IPV4_HOST_OCTETS);
            BBacnetIpLinkLayer.this.myMac[4] = (byte)((BBacnetIpLinkLayer.this.getPort() & 0xFF00) >> 8);
            BBacnetIpLinkLayer.this.myMac[5] = (byte)(BBacnetIpLinkLayer.this.getPort() & 0xFF);
            System.arraycopy(BBacnetIpLinkLayer.this.myMac, 4, BBacnetIpLinkLayer.this.localBroadcastAddr, 4, 2);
            if (BBacnetIpLinkLayer.logger.isLoggable(Level.FINE)) {
               BBacnetIpLinkLayer.logger.fine("Local Broadcast Address:" + ByteArrayUtil.toHexString(BBacnetIpLinkLayer.this.localBroadcastAddr));
            }
         } catch (Exception var8) {
            BBacnetIpLinkLayer.logger.log(Level.SEVERE, "Invalid Bacnet/IP Link layer configuration!", (Throwable)var8);
         }

         return null;
      }
   }
}
