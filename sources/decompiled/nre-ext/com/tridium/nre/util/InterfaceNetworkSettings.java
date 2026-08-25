package com.tridium.nre.util;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class InterfaceNetworkSettings {
   InterfaceAddress interfaceAddress;
   private NetworkInterface networkInterface;
   private static Logger log = Logger.getLogger("ip.util");

   static InterfaceNetworkSettings make(NetworkInterface networkInterface, InterfaceAddress interfaceAddress) {
      return interfaceAddress.getAddress() instanceof Inet4Address
         ? new InterfaceNetworkSettings.InterfaceIPv4NetworkSettings(networkInterface, interfaceAddress)
         : new InterfaceNetworkSettings.InterfaceIPv6NetworkSettings(networkInterface, interfaceAddress);
   }

   private InterfaceNetworkSettings(NetworkInterface networkInterface) {
      this.networkInterface = networkInterface;
   }

   abstract boolean isSameSubnet(InetAddress var1);

   public InetAddress getInetAddress() {
      return this.interfaceAddress.getAddress();
   }

   public InterfaceAddress getInterfaceAddress() {
      return this.interfaceAddress;
   }

   @Override
   public String toString() {
      return this.interfaceAddress.toString();
   }

   public NetworkInterface getNetworkInterface() {
      return this.networkInterface;
   }

   private static class InterfaceIPv4NetworkSettings extends InterfaceNetworkSettings {
      private int ipv4NetworkPacked;
      private int ipv4NetworkMaskPacked;

      InterfaceIPv4NetworkSettings(NetworkInterface networkInterface, InterfaceAddress interfaceAddress) {
         super(networkInterface);
         this.interfaceAddress = interfaceAddress;

         try {
            if (InterfaceNetworkSettings.log.isLoggable(Level.FINEST)) {
               InterfaceNetworkSettings.log.finest("Creating InterfaceNetworkSettings for address \"" + interfaceAddress.getAddress() + "\"...");
            }
         } catch (Exception var7) {
         }

         this.ipv4NetworkMaskPacked = -1 << 32 - interfaceAddress.getNetworkPrefixLength();
         byte[] ipv4SubnetMaskArray = new byte[]{
            (byte)(this.ipv4NetworkMaskPacked >>> 24),
            (byte)(this.ipv4NetworkMaskPacked >> 16 & 0xFF),
            (byte)(this.ipv4NetworkMaskPacked >> 8 & 0xFF),
            (byte)(this.ipv4NetworkMaskPacked & 0xFF)
         };

         try {
            if (InterfaceNetworkSettings.log.isLoggable(Level.FINEST)) {
               InterfaceNetworkSettings.log.finest("Determined netmask \"" + InetAddress.getByAddress(ipv4SubnetMaskArray) + "\" for interface");
            }
         } catch (Exception var6) {
         }

         byte[] ipv4NetworkAddress = interfaceAddress.getAddress().getAddress();
         int ipv4NetworkAddressPacked = 0;
         ipv4NetworkAddressPacked |= ipv4NetworkAddress[0] & 255;
         ipv4NetworkAddressPacked <<= 8;
         ipv4NetworkAddressPacked |= ipv4NetworkAddress[1] & 255;
         ipv4NetworkAddressPacked <<= 8;
         ipv4NetworkAddressPacked |= ipv4NetworkAddress[2] & 255;
         ipv4NetworkAddressPacked <<= 8;
         ipv4NetworkAddressPacked |= ipv4NetworkAddress[3] & 255;
         this.ipv4NetworkPacked = ipv4NetworkAddressPacked & this.ipv4NetworkMaskPacked;
      }

      @Override
      boolean isSameSubnet(InetAddress clientAddress) {
         if (clientAddress instanceof Inet6Address) {
            return false;
         }

         Inet4Address ipv4ClientAddress = (Inet4Address)clientAddress;
         if (this.getInetAddress().equals(ipv4ClientAddress)) {
            return true;
         }

         byte[] ipv4ClientAddressArray = ipv4ClientAddress.getAddress();
         int ipv4ClientAddressPacked = 0;
         ipv4ClientAddressPacked |= ipv4ClientAddressArray[0] & 255;
         ipv4ClientAddressPacked <<= 8;
         ipv4ClientAddressPacked |= ipv4ClientAddressArray[1] & 255;
         ipv4ClientAddressPacked <<= 8;
         ipv4ClientAddressPacked |= ipv4ClientAddressArray[2] & 255;
         ipv4ClientAddressPacked <<= 8;
         ipv4ClientAddressPacked |= ipv4ClientAddressArray[3] & 255;
         int ipv4ClientNetworkPacked = ipv4ClientAddressPacked & this.ipv4NetworkMaskPacked;
         return ipv4ClientNetworkPacked == this.ipv4NetworkPacked;
      }
   }

   private static class InterfaceIPv6NetworkSettings extends InterfaceNetworkSettings {
      private long ipv6NetworkPacked1;
      private long ipv6NetworkPacked2;
      private long ipv6NetworkMaskPacked1;
      private long ipv6NetworkMaskPacked2;
      private int ipv6ScopeId;
      private Inet6Address netBsdLinkLocalCorrection = null;

      InterfaceIPv6NetworkSettings(NetworkInterface networkInterface, InterfaceAddress interfaceAddress) {
         super(networkInterface);
         this.interfaceAddress = interfaceAddress;

         try {
            if (InterfaceNetworkSettings.log.isLoggable(Level.FINEST)) {
               InterfaceNetworkSettings.log.finest("Creating InterfaceNetworkSettings for address \"" + interfaceAddress.getAddress() + "\"...");
            }
         } catch (Exception var13) {
         }

         if (interfaceAddress.getNetworkPrefixLength() > 64) {
            this.ipv6NetworkMaskPacked1 = -1L;
            this.ipv6NetworkMaskPacked2 = -1L << 128 - interfaceAddress.getNetworkPrefixLength();
         } else {
            this.ipv6NetworkMaskPacked1 = -1L << 64 - interfaceAddress.getNetworkPrefixLength();
            this.ipv6NetworkMaskPacked2 = 0L;
         }

         byte[] ipv6NetworkAddress = interfaceAddress.getAddress().getAddress();
         long ipv6NetworkAddressPacked1 = 0L;
         ipv6NetworkAddressPacked1 |= ipv6NetworkAddress[0] & 0xFF;
         ipv6NetworkAddressPacked1 <<= 8;
         ipv6NetworkAddressPacked1 |= ipv6NetworkAddress[1] & 0xFF;
         ipv6NetworkAddressPacked1 <<= 8;
         ipv6NetworkAddressPacked1 |= ipv6NetworkAddress[2] & 0xFF;
         ipv6NetworkAddressPacked1 <<= 8;
         ipv6NetworkAddressPacked1 |= ipv6NetworkAddress[3] & 0xFF;
         ipv6NetworkAddressPacked1 <<= 8;
         ipv6NetworkAddressPacked1 |= ipv6NetworkAddress[4] & 0xFF;
         ipv6NetworkAddressPacked1 <<= 8;
         ipv6NetworkAddressPacked1 |= ipv6NetworkAddress[5] & 0xFF;
         ipv6NetworkAddressPacked1 <<= 8;
         ipv6NetworkAddressPacked1 |= ipv6NetworkAddress[6] & 0xFF;
         ipv6NetworkAddressPacked1 <<= 8;
         ipv6NetworkAddressPacked1 |= ipv6NetworkAddress[7] & 0xFF;
         long ipv6NetworkAddressPacked2 = 0L;
         ipv6NetworkAddressPacked2 |= ipv6NetworkAddress[8] & 0xFF;
         ipv6NetworkAddressPacked2 <<= 8;
         ipv6NetworkAddressPacked2 |= ipv6NetworkAddress[9] & 0xFF;
         ipv6NetworkAddressPacked2 <<= 8;
         ipv6NetworkAddressPacked2 |= ipv6NetworkAddress[10] & 0xFF;
         ipv6NetworkAddressPacked2 <<= 8;
         ipv6NetworkAddressPacked2 |= ipv6NetworkAddress[11] & 0xFF;
         ipv6NetworkAddressPacked2 <<= 8;
         ipv6NetworkAddressPacked2 |= ipv6NetworkAddress[12] & 0xFF;
         ipv6NetworkAddressPacked2 <<= 8;
         ipv6NetworkAddressPacked2 |= ipv6NetworkAddress[13] & 0xFF;
         ipv6NetworkAddressPacked2 <<= 8;
         ipv6NetworkAddressPacked2 |= ipv6NetworkAddress[14] & 0xFF;
         ipv6NetworkAddressPacked2 <<= 8;
         ipv6NetworkAddressPacked2 |= ipv6NetworkAddress[15] & 0xFF;
         this.ipv6NetworkPacked1 = ipv6NetworkAddressPacked1 & this.ipv6NetworkMaskPacked1;
         this.ipv6NetworkPacked2 = ipv6NetworkAddressPacked2 & this.ipv6NetworkMaskPacked2;
         Inet6Address ipv6Address = (Inet6Address)interfaceAddress.getAddress();
         this.ipv6ScopeId = ipv6Address.getScopeId();
         if (ipv6Address.isLinkLocalAddress() && this.ipv6ScopeId == 0) {
            byte[] ipv6AddressBytes = ipv6Address.getAddress();
            int alternateScopeID = 0;
            alternateScopeID |= ipv6AddressBytes[2] & 255;
            alternateScopeID <<= 8;
            alternateScopeID |= ipv6AddressBytes[3] & 255;
            if (alternateScopeID != 0) {
               try {
                  byte[] correctedAddress = ipv6Address.getAddress();
                  correctedAddress[2] = 0;
                  correctedAddress[3] = 0;
                  this.netBsdLinkLocalCorrection = Inet6Address.getByAddress(IPAddressUtil.getHostName(), correctedAddress, alternateScopeID);
                  this.ipv6ScopeId = alternateScopeID;
                  this.ipv6NetworkPacked1 &= -108086391056891904L;
               } catch (Exception var12) {
               }
            }
         }
      }

      @Override
      boolean isSameSubnet(InetAddress clientAddress) {
         if (clientAddress instanceof Inet4Address) {
            return false;
         }

         Inet6Address ipv6ClientAddress = (Inet6Address)clientAddress;
         if (this.getInetAddress().equals(ipv6ClientAddress)) {
            return true;
         }

         if (ipv6ClientAddress.getScopeId() > 0 && this.ipv6ScopeId > 0) {
            return ipv6ClientAddress.getScopeId() == this.ipv6ScopeId;
         }

         byte[] ipv6ClientAddressArray = ipv6ClientAddress.getAddress();
         long ipv6ClientAddressPacked1 = 0L;
         ipv6ClientAddressPacked1 |= ipv6ClientAddressArray[0] & 0xFF;
         ipv6ClientAddressPacked1 <<= 8;
         ipv6ClientAddressPacked1 |= ipv6ClientAddressArray[1] & 0xFF;
         ipv6ClientAddressPacked1 <<= 8;
         ipv6ClientAddressPacked1 |= ipv6ClientAddressArray[2] & 0xFF;
         ipv6ClientAddressPacked1 <<= 8;
         ipv6ClientAddressPacked1 |= ipv6ClientAddressArray[3] & 0xFF;
         ipv6ClientAddressPacked1 <<= 8;
         ipv6ClientAddressPacked1 |= ipv6ClientAddressArray[4] & 0xFF;
         ipv6ClientAddressPacked1 <<= 8;
         ipv6ClientAddressPacked1 |= ipv6ClientAddressArray[5] & 0xFF;
         ipv6ClientAddressPacked1 <<= 8;
         ipv6ClientAddressPacked1 |= ipv6ClientAddressArray[6] & 0xFF;
         ipv6ClientAddressPacked1 <<= 8;
         ipv6ClientAddressPacked1 |= ipv6ClientAddressArray[7] & 0xFF;
         long ipv6ClientAddressPacked2 = 0L;
         ipv6ClientAddressPacked2 |= ipv6ClientAddressArray[8] & 0xFF;
         ipv6ClientAddressPacked2 <<= 8;
         ipv6ClientAddressPacked2 |= ipv6ClientAddressArray[9] & 0xFF;
         ipv6ClientAddressPacked2 <<= 8;
         ipv6ClientAddressPacked2 |= ipv6ClientAddressArray[10] & 0xFF;
         ipv6ClientAddressPacked2 <<= 8;
         ipv6ClientAddressPacked2 |= ipv6ClientAddressArray[11] & 0xFF;
         ipv6ClientAddressPacked2 <<= 8;
         ipv6ClientAddressPacked2 |= ipv6ClientAddressArray[12] & 0xFF;
         ipv6ClientAddressPacked2 <<= 8;
         ipv6ClientAddressPacked2 |= ipv6ClientAddressArray[13] & 0xFF;
         ipv6ClientAddressPacked2 <<= 8;
         ipv6ClientAddressPacked2 |= ipv6ClientAddressArray[14] & 0xFF;
         ipv6ClientAddressPacked2 <<= 8;
         ipv6ClientAddressPacked2 |= ipv6ClientAddressArray[15] & 0xFF;
         long ipv6ClientNetworkPacked1 = ipv6ClientAddressPacked1 & this.ipv6NetworkMaskPacked1;
         long ipv6ClientNetworkPacked2 = ipv6ClientAddressPacked2 & this.ipv6NetworkMaskPacked2;
         return ipv6ClientNetworkPacked1 == this.ipv6NetworkPacked1 && ipv6ClientNetworkPacked2 == this.ipv6NetworkPacked2;
      }

      @Override
      public InetAddress getInetAddress() {
         return this.netBsdLinkLocalCorrection != null ? this.netBsdLinkLocalCorrection : this.interfaceAddress.getAddress();
      }
   }
}
