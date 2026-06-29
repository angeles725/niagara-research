package com.tridium.bacnet.stack.network;

import com.tridium.bacnet.stack.BacnetInputStream;
import com.tridium.bacnet.stack.BacnetStackException;
import com.tridium.bacnet.stack.network.messages.ApplicationMsg;
import com.tridium.bacnet.stack.network.messages.DisconnectConnectionToNetwork;
import com.tridium.bacnet.stack.network.messages.EstablishConnectionToNetwork;
import com.tridium.bacnet.stack.network.messages.IAmRouterToNetwork;
import com.tridium.bacnet.stack.network.messages.ICouldBeRouterToNetwork;
import com.tridium.bacnet.stack.network.messages.InitializeRoutingTable;
import com.tridium.bacnet.stack.network.messages.InitializeRoutingTableAck;
import com.tridium.bacnet.stack.network.messages.NetworkLayerMsg;
import com.tridium.bacnet.stack.network.messages.NetworkNumberIs;
import com.tridium.bacnet.stack.network.messages.RejectMessageToNetwork;
import com.tridium.bacnet.stack.network.messages.RouterAvailableToNetwork;
import com.tridium.bacnet.stack.network.messages.RouterBusyToNetwork;
import com.tridium.bacnet.stack.network.messages.WhatIsNetworkNumber;
import com.tridium.bacnet.stack.network.messages.WhoIsRouterToNetwork;
import com.tridium.bacnet.timers.Timers;
import java.io.ByteArrayOutputStream;
import javax.baja.bacnet.datatypes.BBacnetAddress;
import javax.baja.nre.util.ByteArrayUtil;

public abstract class NetworkPdu implements NetworkMsgType, PrioritizedQueueEntry {
   public static final int VERSION = 1;
   public static final int NETWORK_LAYER_MSG_BIT = 128;
   public static final int DEST_SPECIFIER_BIT = 32;
   public static final int SOURCE_SPECIFIER_BIT = 8;
   public static final int DATA_EXPECTING_REPLY_BIT = 4;
   public static final int NETWORK_PRIORITY_MASK = 3;
   public static final int DEFAULT_HOP_COUNT = 255;
   public static final int DNET_NOT_PRESENT = -1;
   public static final int SNET_NOT_PRESENT = -1;
   public static final int NOT_A_NETWORK_MESSAGE = -1;
   private int version;
   private BNetworkPriority networkPriority;
   private boolean dataExpectingReply;
   private int dnet = -1;
   private byte[] dadr;
   private int snet = -1;
   private byte[] sadr;
   private int hopCount;
   private boolean isBroadcast;
   private DataAttributes dataAttributes;
   private byte[] srcMacAddress;
   private int srcNetworkNumber = 0;
   private byte[] destMacAddress;
   private int destNetworkNumber = 0;
   private BNetworkPort sourcePort;
   private PrioritizedQueueEntry next;

   protected NetworkPdu() {
      this.version = 1;
      this.hopCount = 255;
      this.networkPriority = BNetworkPriority.normal;
   }

   protected NetworkPdu(BBacnetAddress destAddr, BBacnetAddress sourceAddr, BNetworkPriority networkPriority, boolean dataExpectingReply) {
      if (sourceAddr != null) {
         this.snet = sourceAddr.getNetworkNumber();
         this.sadr = sourceAddr.getMacAddress().getBytes();
      } else {
         this.snet = -1;
      }

      if (destAddr != null) {
         this.dnet = destAddr.getNetworkNumber();
         this.dadr = destAddr.getMacAddress().getBytes();
      } else {
         this.dnet = -1;
      }

      this.networkPriority = networkPriority;
      this.dataExpectingReply = dataExpectingReply;
      this.version = 1;
      this.hopCount = 255;
   }

   public int getMessageType() {
      return -1;
   }

   public int getVersion() {
      return this.version;
   }

   public abstract boolean isNetworkLayerMsg();

   public boolean getDataExpectingReply() {
      return this.dataExpectingReply;
   }

   public void setDataExpectingReply(boolean dataExpectingReply) {
      this.dataExpectingReply = dataExpectingReply;
   }

   public BNetworkPriority getNetworkPriority() {
      return this.networkPriority;
   }

   public void setNetworkPriority(BNetworkPriority networkPriority) {
      this.networkPriority = networkPriority;
   }

   public boolean isDNET() {
      return this.dnet != -1;
   }

   public int getDNET() {
      return this.dnet;
   }

   public void setDNET(int dnet) {
      this.dnet = dnet;
   }

   public int getDLEN() {
      return this.dadr == null ? 0 : this.dadr.length;
   }

   public byte[] getDADR() {
      return this.dadr;
   }

   public void setDADR(byte[] dadr) {
      this.dadr = dadr;
   }

   public boolean isSNET() {
      return this.snet != -1;
   }

   public int getSNET() {
      return this.snet;
   }

   public void setSNET(int snet) {
      this.snet = snet;
   }

   public int getSLEN() {
      return this.sadr == null ? 0 : this.sadr.length;
   }

   public byte[] getSADR() {
      return this.sadr;
   }

   public void setSADR(byte[] sadr) {
      this.sadr = sadr;
   }

   public int getHopCount() {
      return this.hopCount;
   }

   public void decrementHopCount() {
      this.hopCount--;
   }

   public BBacnetAddress getSrcAddress() {
      return this.snet == -1 ? new BBacnetAddress(this.srcNetworkNumber, this.srcMacAddress) : new BBacnetAddress(this.snet, this.sadr);
   }

   public byte[] getSrcMacAddress() {
      return this.srcMacAddress;
   }

   public int getSrcNetworkNumber() {
      return this.srcNetworkNumber;
   }

   public BBacnetAddress getDestAddress() {
      return this.dnet == -1 ? new BBacnetAddress(this.destNetworkNumber, this.destMacAddress) : new BBacnetAddress(this.dnet, this.dadr);
   }

   public byte[] getDestMacAddress() {
      return this.destMacAddress;
   }

   public int getDestNetworkNumber() {
      return this.destNetworkNumber;
   }

   public BNetworkPort getSourcePort() {
      return this.sourcePort;
   }

   public void setSourcePort(BNetworkPort sourcePort) {
      this.sourcePort = sourcePort;
   }

   public void writeNetworkBytes(ByteArrayOutputStream os) {
      os.write(1);
      int control = 0;
      if (this.isNetworkLayerMsg()) {
         control |= 128;
      }

      if (this.dnet != -1) {
         control |= 32;
      }

      if (this.snet != -1) {
         control |= 8;
      }

      if (this.dataExpectingReply) {
         control |= 4;
      }

      control |= this.networkPriority.getOrdinal();
      os.write(control);
      if ((control & 32) != 0) {
         os.write(this.dnet >> 8);
         os.write(this.dnet);
         if (this.dnet != 65535) {
            if (this.dadr != null) {
               os.write(this.dadr.length);
               os.write(this.dadr, 0, this.dadr.length);
            } else {
               os.write(0);
            }
         } else {
            os.write(0);
         }
      }

      if ((control & 8) != 0) {
         os.write(this.snet >> 8);
         os.write(this.snet);
         os.write(this.sadr.length);
         os.write(this.sadr, 0, this.sadr.length);
      }

      if ((control & 32) != 0) {
         os.write(this.hopCount);
      }
   }

   public static NetworkPdu parseNetworkBytes(byte[] srcMacAddress, byte[] destMacAddress, int networkNumber, BacnetInputStream is, boolean isBroadcast) throws BacnetStackException {
      int version = is.read();
      if (version != 1) {
         throw new UnsupportedProtocolVersionException(version);
      } else {
         int control = is.read();
         BNetworkPriority networkPriority = BNetworkPriority.make(control & 3);
         boolean dataExpectingReply = (control & 4) != 0;
         byte[] dadr;
         int dnet;
         if ((control & 32) != 0) {
            int temp = is.read();
            dnet = is.read();
            dnet = temp << 8 | dnet;
            dadr = new byte[is.read()];
            is.read(dadr, 0, dadr.length);
         } else {
            dnet = -1;
            dadr = null;
         }

         byte[] sadr;
         int snet;
         if ((control & 8) != 0) {
            int temp = is.read();
            snet = is.read();
            snet = temp << 8 | snet;
            sadr = new byte[is.read()];
            is.read(sadr, 0, sadr.length);
         } else {
            snet = -1;
            sadr = null;
         }

         int hopCount;
         if ((control & 32) != 0) {
            hopCount = is.read();
         } else {
            hopCount = 0;
         }

         NetworkPdu npdu;
         if ((control & 128) != 0) {
            int messageType = is.read();
            if ((messageType & 128) != 0) {
               int vendorId = is.read() << 8 | is.read();
               npdu = new NetworkLayerMsg(messageType, vendorId, is);
            } else if (messageType >= 20 && messageType <= 127) {
               npdu = new NetworkLayerMsg(messageType, is);
            } else {
               switch (messageType) {
                  case 0:
                     npdu = new WhoIsRouterToNetwork(is);
                     break;
                  case 1:
                     npdu = new IAmRouterToNetwork(is);
                     break;
                  case 2:
                     npdu = new ICouldBeRouterToNetwork(is);
                     break;
                  case 3:
                     npdu = new RejectMessageToNetwork(is);
                     break;
                  case 4:
                     npdu = new RouterBusyToNetwork(is);
                     break;
                  case 5:
                     npdu = new RouterAvailableToNetwork(is);
                     break;
                  case 6:
                     npdu = new InitializeRoutingTable(is);
                     break;
                  case 7:
                     npdu = new InitializeRoutingTableAck(is);
                     break;
                  case 8:
                     npdu = new EstablishConnectionToNetwork(is);
                     break;
                  case 9:
                     npdu = new DisconnectConnectionToNetwork(is);
                     break;
                  case 10:
                  case 11:
                  case 12:
                  case 13:
                  case 14:
                  case 15:
                  case 16:
                  case 17:
                  default:
                     throw new UnsupportedNetworkMsgException(messageType);
                  case 18:
                     npdu = new WhatIsNetworkNumber(networkNumber);
                     break;
                  case 19:
                     npdu = new NetworkNumberIs(is, networkNumber);
               }
            }

            is.release();
         } else {
            npdu = new ApplicationMsg(is);
         }

         npdu.version = version;
         npdu.dataExpectingReply = dataExpectingReply;
         npdu.dnet = dnet;
         npdu.dadr = dadr;
         npdu.snet = snet;
         npdu.sadr = sadr;
         npdu.hopCount = hopCount;
         npdu.networkPriority = networkPriority;
         npdu.isBroadcast = isBroadcast;
         npdu.srcMacAddress = srcMacAddress;
         npdu.srcNetworkNumber = networkNumber;
         npdu.destMacAddress = destMacAddress;
         npdu.destNetworkNumber = networkNumber;
         return npdu;
      }
   }

   public DataAttributes getDataAttributes() {
      return this.dataAttributes;
   }

   public void setDataAttributes(DataAttributes dataAttributes) {
      this.dataAttributes = dataAttributes;
   }

   @Override
   public final String toString() {
      StringBuilder sb = new StringBuilder("Npdu: ");
      sb.append("ver: " + this.version);
      if (this.isNetworkLayerMsg()) {
         sb.append("  type: net");
      } else {
         sb.append("  type: app");
      }

      sb.append("  pri: " + this.networkPriority);
      sb.append("  DER: " + this.dataExpectingReply);
      if (this.isDNET()) {
         sb.append("\n      DNET: " + this.dnet);
         if (this.dadr == null) {
            sb.append("  DLEN: 0");
            sb.append("  DADR: N/A");
         } else {
            sb.append("  DLEN: " + this.dadr.length);
            sb.append("  DADR: ");
            if (this.dadr.length == 0) {
               sb.append("bcast");
            } else {
               sb.append(ByteArrayUtil.toHexString(this.dadr));
            }

            sb.append("  hopCnt: " + this.hopCount);
         }
      }

      if (this.isSNET()) {
         sb.append("\n      SNET: " + this.snet);
         if (this.sadr == null) {
            sb.append("  SLEN: 0");
            sb.append("  SADR: N/A");
         } else {
            sb.append("  SLEN: " + this.getSLEN());
            sb.append("  SADR: " + ByteArrayUtil.toHexString(this.sadr));
         }
      }

      if (this.isNetworkLayerMsg()) {
         sb.append("\n  netMsg: " + ((NetworkLayerMsg)this).getMsgString());
      }

      return sb.toString();
   }

   @Override
   public PrioritizedQueueEntry getNext() {
      return this.next;
   }

   @Override
   public void setNext(PrioritizedQueueEntry e) {
      this.next = e;
   }

   @Override
   public int getPriority() {
      return this.networkPriority.getOrdinal();
   }

   public int startTimer(BBacnetNetworkLayer net) {
      return Timers.add(net, net.getRouterDiscoveryTimeout(), this);
   }

   public boolean isBroadcast() {
      return this.isBroadcast;
   }
}
