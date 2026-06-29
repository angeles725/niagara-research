package com.tridium.bacnet.util.tap;

import com.tridium.bacnet.stack.BacnetInputStream;
import com.tridium.bacnet.stack.link.ethernet.BBacnetEthernetLinkLayer;
import com.tridium.bacnet.stack.link.ip.BBacnetIpLinkLayer;
import com.tridium.bacnet.stack.link.ip.OriginalUnicastNpdu;
import com.tridium.bacnet.stack.network.BNetworkPort;
import com.tridium.bacnet.stack.network.NetworkPdu;
import com.tridium.bacnet.stack.network.messages.ApplicationMsg;
import com.tridium.bacnet.stack.network.wiretap.IncomingWiretap;
import com.tridium.bacnet.stack.network.wiretap.OutgoingWiretap;
import com.tridium.bacnet.stack.network.wiretap.WiretapAware;
import com.tridium.bacnet.util.point.BPooledWorkerPoint;
import com.tridium.bacnet.util.point.EventsPerSecond;
import java.io.ByteArrayOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.bacnet.datatypes.BBacnetAddress;
import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.status.BStatusNumeric;
import javax.baja.status.BStatusValue;
import javax.baja.sys.BComponent;
import javax.baja.sys.BFacets;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.units.BUnit;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "facets",
      type = "BFacets",
      defaultValue = "BFacets.makeNumeric(BUnit.getUnit(\"per second\"), 1)",
      override = true
   ), @NiagaraProperty(
      name = "address",
      type = "String",
      defaultValue = "192.168.1.255"
   ), @NiagaraProperty(
      name = "port",
      type = "int",
      defaultValue = "49000",
      facets = {@Facet(
         name = "BFacets.MAX",
         value = "65535"
      ), @Facet(
         name = "BFacets.MIN",
         value = "49000"
      )}
   )})
public class BForwardingWiretap extends BPooledWorkerPoint implements IncomingWiretap, OutgoingWiretap, EventsPerSecond {
   public static final Property facets = newProperty(0, BFacets.makeNumeric(BUnit.getUnit("per second"), 1), null);
   public static final Property address = newProperty(0, "192.168.1.255", null);
   public static final Property port = newProperty(0, 49000, BFacets.make(BFacets.make("max", 65535), BFacets.make("min", 49000)));
   public static final Type TYPE = Sys.loadType(BForwardingWiretap.class);
   private volatile Integer portNumber = null;
   private volatile DatagramSocket socket = null;
   private volatile InetAddress inetAddress = null;
   private AtomicLong forwardedMessages = new AtomicLong();
   private long lastUpdate = System.currentTimeMillis();
   private static final Logger logger = Logger.getLogger("bacnetUtil.tap.forwarding.wiretap");
   private static int IP_MTU = 1470;
   private static int ETH_SAP = 130;
   private static int ETH_ADDR_LEN = 6;
   private static int ETH_FRAME_LEN = 2;
   private static int ETH_DSAP = 1;
   private static int ETH_SSAP = 1;
   private static int ETH_CONTROL = 1;
   private static int ETH_DSAP_IDX = 2 * ETH_ADDR_LEN + ETH_FRAME_LEN;
   private static int ETH_HEADER_LENGTH = ETH_DSAP_IDX + ETH_DSAP + ETH_SSAP + ETH_CONTROL;

   public String getAddress() {
      return this.getString(address);
   }

   public void setAddress(String v) {
      this.setString(address, v, null);
   }

   public int getPort() {
      return this.getInt(port);
   }

   public void setPort(int v) {
      this.setInt(port, v, null);
   }

   @Override
   public Type getType() {
      return TYPE;
   }

   public boolean isParentLegal(BComponent parent) {
      return parent instanceof WiretapAware;
   }

   @Override
   public void started() throws Exception {
      super.started();
      this.restartSocket();
   }

   @Override
   public void stopped() throws Exception {
      super.stopped();
      this.stopSocket();
   }

   @Override
   public void changed(Property property, Context cx) {
      if (property.equals(address) || property.equals(port) || property.equals(enabled)) {
         this.portNumber = null;
         this.inetAddress = null;
         this.restartSocket();
      }

      super.changed(property, cx);
   }

   private void stopSocket() {
      if (this.socket != null) {
         this.socket.close();
      }
   }

   private void restartSocket() {
      this.stopSocket();
      if (this.getEnabled()) {
         try {
            this.socket = AccessController.doPrivileged(new PrivilegedAction<DatagramSocket>() {
               public DatagramSocket run() {
                  try {
                     return new DatagramSocket();
                  } catch (SocketException var2x) {
                     BForwardingWiretap.logger.log(Level.SEVERE, "SocketException occurred in restartSocket", (Throwable)var2x);
                     return null;
                  }
               }
            });
         } catch (NumberFormatException var2) {
            logger.log(Level.SEVERE, "NumberFormatException occurred in restartSocket", (Throwable)var2);
         }
      }
   }

   public void onExecute(BStatusValue o, Context cx) {
      long t1 = this.lastUpdate;
      this.lastUpdate = System.currentTimeMillis();
      long messagesForwarded = this.forwardedMessages.getAndSet(0L);
      BStatusNumeric out = (BStatusNumeric)o;
      out.setValue(this.calculateEventsPerSecond(messagesForwarded, t1, this.lastUpdate));
   }

   public void rcvIndication(byte[] srcMacAddress, byte[] destMacAddress, BacnetInputStream is, boolean isBroadcast) {
      this.forwardedMessages.incrementAndGet();
      if (this.getEnabled()) {
         this.run(() -> {
            if (this.socket != null) {
               BacnetInputStream copy = is.copy();
               BNetworkPort port = (BNetworkPort)this.getParent();
               NetworkPdu npdu = null;

               try {
                  if (port.getLink() instanceof BBacnetIpLinkLayer) {
                     if (!this.stripBlvc(copy)) {
                        return;
                     }
                  } else if (port.getLink() instanceof BBacnetEthernetLinkLayer && !this.stripEthHeader(copy)) {
                     return;
                  }

                  npdu = NetworkPdu.parseNetworkBytes(srcMacAddress, destMacAddress, port.getNetworkNumber(), copy, isBroadcast);
                  this.setAddresses(npdu, port.getNetworkNumber(), srcMacAddress, destMacAddress);
                  if (npdu instanceof ApplicationMsg) {
                     ApplicationMsg appMsg = (ApplicationMsg)npdu;
                     appMsg.storeApdu();
                  }

                  OriginalUnicastNpdu unicast = new OriginalUnicastNpdu(npdu);
                  this.send(unicast);
               } catch (Exception var9) {
                  logger.log(Level.SEVERE, "Exception occurred in rcvIndication", (Throwable)var9);
               }
            }
         });
      }
   }

   public boolean stripBlvc(BacnetInputStream is) {
      int type = is.read();
      if (type != 129) {
         return false;
      } else {
         int function = is.read();
         switch (function) {
            case 10:
            case 11:
               is.read();
               is.read();
               return true;
            default:
               return false;
         }
      }
   }

   public boolean stripEthHeader(BacnetInputStream is) {
      if (is != null && is.available() > ETH_HEADER_LENGTH) {
         is.skip(ETH_DSAP_IDX);
         int dsap = is.read();
         if (dsap != ETH_SAP) {
            return false;
         } else {
            int ssap = is.read();
            if (ssap != ETH_SAP) {
               return false;
            } else {
               is.skip(ETH_CONTROL);
               return true;
            }
         }
      } else {
         return false;
      }
   }

   public void sendRequest(byte[] destAddress, NetworkPdu npdu) {
      this.forwardedMessages.incrementAndGet();
      if (this.getEnabled()) {
         this.run(() -> {
            BNetworkPort port = (BNetworkPort)this.getParent();
            BBacnetAddress address = port.getAddress();
            byte[] srcMacAddress = address.getMacAddress().getBytes();
            this.setAddresses(npdu, port.getNetworkNumber(), srcMacAddress, destAddress);
            OriginalUnicastNpdu unicast = new OriginalUnicastNpdu(npdu);
            this.send(unicast);
         });
      }
   }

   private void send(OriginalUnicastNpdu npdu) {
      if (npdu != null) {
         ByteArrayOutputStream baos = new ByteArrayOutputStream();
         byte[] packetData = npdu.encode(baos);
         DatagramPacket packet = new DatagramPacket(packetData, Math.min(packetData.length, IP_MTU), this.getInetAddress(), this.getPortNumber());

         try {
            this.socket.send(packet);
         } catch (Exception var6) {
            logger.log(Level.SEVERE, "Exception occurred in send", (Throwable)var6);
         }
      }
   }

   private void setAddresses(NetworkPdu npdu, int networkNumber, byte[] srcAddress, byte[] destAddress) {
      if (!npdu.isDNET()) {
         npdu.setDNET(networkNumber);
         npdu.setDADR(destAddress);
      }

      if (!npdu.isSNET()) {
         npdu.setSNET(networkNumber);
         npdu.setSADR(srcAddress);
      }
   }

   private InetAddress getInetAddress() {
      if (this.inetAddress == null) {
         try {
            this.inetAddress = InetAddress.getByName(this.getAddress());
         } catch (UnknownHostException var2) {
            logger.log(Level.SEVERE, "UnknownHostException occurred in getInetAddress", (Throwable)var2);
         }
      }

      return this.inetAddress;
   }

   private int getPortNumber() {
      if (this.portNumber == null) {
         this.portNumber = this.getPort();
      }

      return this.portNumber;
   }
}
