package com.tridium.bacnet.stack.link.ip.util;

public class BacnetIpLinkUtil {
   public static final String NON_ROUTABLE_ADDRESS = "0.0.0.0";
   public static final int IP_MAC_LEN = 6;

   public static boolean isSourceLocal(byte[] source, byte[] local, short mask) {
      if (source != null && local != null && source.length == 6 && local.length == 6) {
         int sourceAddress = convertMacToInt(source);
         int localAddress = convertMacToInt(local);
         int netmask = convertNetmask(mask);
         if ((sourceAddress & netmask) == (localAddress & netmask)) {
            return sameUdpPort(source, local);
         }
      }

      return false;
   }

   public static int convertNetmask(short networkPrefix) {
      if (networkPrefix <= 0) {
         return 0;
      } else {
         int offset = 32 - networkPrefix;
         return -1 >> offset << offset;
      }
   }

   public static boolean sameUdpPort(byte[] macAddress, byte[] otherMacAddress) {
      return macAddress[4] == otherMacAddress[4] && macAddress[5] == otherMacAddress[5];
   }

   public static int convertMacToInt(byte[] mac) {
      int intMac = 0;
      if (mac.length > 3) {
         intMac = mac[0] << 24 & 0xFF000000 | mac[1] << 16 & 0xFF0000 | mac[2] << 8 & 0xFF00 | mac[3] & 255;
      }

      return intMac;
   }

   public static byte[] convertIntToAddress(int address) {
      return new byte[]{(byte)(0xFF & address >> 24), (byte)(0xFF & address >> 16), (byte)(0xFF & address >> 8), (byte)(0xFF & address)};
   }

   public static byte[] getBroadcastAddress(byte[] address, short networkPrefix) {
      return convertIntToAddress(convertMacToInt(address) | ~convertNetmask(networkPrefix));
   }
}
