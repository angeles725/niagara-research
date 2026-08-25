package com.tridium.nre.util;

import com.tridium.nre.platform.IPlatformProvider;
import com.tridium.nre.platform.PlatformUtil;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javax.baja.nre.util.TextUtil;

public abstract class IPAddressUtil {
   public static final String NIAGARA_IPV6_PROPERTY = "niagara.ipv6Enabled";
   public static final String PREFERRED_ADAPTER_NAME_PROPERTY = "niagara.preferred.network.adapter";
   public static final String DEFAULT_PREFERRED_ADAPTER_NAME = "";
   public static final String VIRTUAL_ADAPTER_NAME_PATTERNS_PROPERTY = "niagara.virtual.network.adapters";
   public static final String DEFAULT_VIRTUAL_ADAPTER_NAME_PATTERNS = "^docker[0-9]+;^virbr[0-9]+;^virt[0-9]+;^vnet[0-9]+";
   public static final String EXCLUDED_ADAPTER_NAME_PATTERNS_PROPERTY = "niagara.excluded.network.adapters";
   public static final String DEFAULT_EXCLUDED_ADAPTER_NAME_PATTERNS = "";
   public static final String ENABLE_NETWORK_INTERFACE_EVENTS_PROPERTY = "niagara.enable.network.interface.events";
   public static final String DEFAULT_ENABLE_NETWORK_INTERFACE_EVENTS = "false";
   private static final String PREFERRED_ADAPTER_NAME = AccessController.doPrivileged(() -> System.getProperty("niagara.preferred.network.adapter", ""));
   private static final Set<String> VIRTUAL_ADAPTER_NAME_PATTERNS = new HashSet<>();
   private static final Set<String> EXCLUDED_ADAPTER_NAME_PATTERNS = new HashSet<>();
   private static final boolean NIAGARA_USES_IPV6 = AccessController.doPrivileged(() -> Boolean.getBoolean("niagara.ipv6Enabled"));
   public static final Comparator<InetAddress> INET_ADDRESS_COMPARATOR = new Comparator<InetAddress>() {
      public int compare(InetAddress LHS, InetAddress RHS) {
         if (LHS instanceof Inet4Address && RHS instanceof Inet6Address) {
            return IPAddressUtil.NIAGARA_USES_IPV6 ? 1 : -1;
         } else if (LHS instanceof Inet6Address && RHS instanceof Inet4Address) {
            return IPAddressUtil.NIAGARA_USES_IPV6 ? -1 : 1;
         } else {
            return 0;
         }
      }
   };
   private static final Comparator<InterfaceNetworkSettings> INTERFACE_COMPARATOR = (LHS, RHS) -> INET_ADDRESS_COMPARATOR.compare(
      LHS.getInetAddress(), RHS.getInetAddress()
   );
   public static final Comparator<Object> ASCENDING = IPAddressUtil::compare;
   public static final Comparator<Object> DESCENDING = ASCENDING.reversed();
   private static final String HEX = "0123456789abcdefABCDEF";
   private static final String ALPHA_NUM = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
   private static InterfaceNetworkSettings[] localhostInterfaces = null;
   private static int addressedAdapters = 0;
   private static final WeakHashMap<InetAddress, InterfaceNetworkSettings> localhostHints = new WeakHashMap<>();
   private static volatile String hostName = null;
   private static volatile InetAddress localHost = null;
   private static volatile InetAddress localHost6 = null;
   private static final Object localHostLock = new Object();
   private static final Logger log = Logger.getLogger("ip.util");

   public static boolean isValidHost(String hostname) {
      return isNumericAddr(hostname) || isHostname(hostname);
   }

   public static boolean isValidHostFormat(String hostname) {
      return isNumericAddrFormat(hostname) || isHostname(hostname);
   }

   public static boolean isNumericAddr(String hostname) {
      return isIpv4Address(hostname) || isIpv6Address(hostname) || isIpv4MappedAddress(hostname);
   }

   public static boolean isNumericAddrFormat(String hostname) {
      return isIpv4Address(hostname) || isIpv6AddressFormat(hostname) || isIpv4MappedAddress(hostname);
   }

   public static boolean isHostname(String address) {
      if (address == null) {
         return false;
      }

      if (address.length() == 0) {
         return false;
      }

      if (address.length() > 255) {
         return false;
      }

      String[] labels = TextUtil.split(address, '.');
      String[] var2 = labels;
      int var3 = var2.length;
      int var4 = 0;

      while (var4 < var3) {
         String currentLabel = var2[var4];
         if (currentLabel.length() >= 1 && currentLabel.length() <= 63) {
            if ("0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".indexOf(currentLabel.charAt(0)) != -1
               && "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".indexOf(currentLabel.charAt(currentLabel.length() - 1)) != -1) {
               for (int idx = 1; idx < currentLabel.length() - 1; idx++) {
                  char currentChar = currentLabel.charAt(idx);
                  if ("0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".indexOf(currentChar) == -1 && currentChar != '-') {
                     return false;
                  }
               }

               var4++;
               continue;
            }

            return false;
         }

         return false;
      }

      return true;
   }

   public static boolean isIpv6Address(String address) {
      return isIpv6Address(address, true);
   }

   public static boolean isIpv6AddressFormat(String address) {
      return isIpv6Address(address, false);
   }

   private static boolean isIpv6Address(String address, boolean validateScope) {
      if (address == null) {
         return false;
      }

      if (address.trim().length() == 0) {
         return false;
      }

      if (!address.contains(":")) {
         return false;
      }

      short colons = 0;
      short width = 0;
      if (address.indexOf("::") != address.lastIndexOf("::")) {
         return false;
      }

      for (short idx = 0; idx < address.length(); idx++) {
         char c = address.charAt(idx);
         if (c == ':') {
            if (colons >= 7) {
               return false;
            }

            width = 0;
            colons++;
         } else if ("0123456789abcdefABCDEF".indexOf(c) != -1) {
            if (width >= 4) {
               return false;
            }

            width++;
         } else {
            if (c != '%') {
               return false;
            }

            if (idx + 1 == address.length()) {
               return false;
            }

            String deviceId = address.substring(idx + 1);
            if (validateScope && !isInteger(deviceId) && !isValidNetworkInterfaceName(deviceId)) {
               return false;
            }

            idx = (short)address.length();
         }
      }

      return colons >= 2
         && (address.contains("::") || colons >= 7)
         && (width != 0 || address.lastIndexOf(":") != address.length() - 1 || address.indexOf("::") == address.length() - 2);
   }

   public static boolean isIpv4MappedAddress(String address) {
      if (address == null) {
         return false;
      } else if (address.trim().length() == 0) {
         return false;
      } else {
         return (address.startsWith("::ffff:") || address.startsWith("::FFFF:")) && address.length() >= 8 ? isIpv4Address(address.substring(7)) : false;
      }
   }

   public static boolean isIpv4Address(String address) {
      if (address == null) {
         return false;
      }

      if (address.trim().length() == 0) {
         return false;
      }

      if (!address.contains(".")) {
         return false;
      }

      int dots = 0;
      int digit = 0;
      int quartetValue = 0;

      for (int i = 0; i < address.length(); i++) {
         char c = address.charAt(i);
         if (c == '.') {
            if (digit == 0 || dots >= 3) {
               return false;
            }

            digit = 0;
            quartetValue = 0;
            dots++;
         } else {
            if (!Character.isDigit(c)) {
               return false;
            }

            if (digit >= 3) {
               return false;
            }

            digit++;
            if ((quartetValue = quartetValue * 10 + Integer.parseInt(String.valueOf(c))) > 255) {
               return false;
            }
         }
      }

      return dots == 3 && digit != 0;
   }

   public static boolean isIpv4SubnetMask(String address) {
      if (address == null) {
         return false;
      }

      if (!isIpv4Address(address)) {
         return false;
      }

      byte[] mask = numericStringToByteArray(address);
      if (mask == null) {
         return false;
      }

      boolean notCompleteHit = false;

      for (int i = 0; i < 4; i++) {
         int unsignedValue = mask[i] & 255;
         if (notCompleteHit && unsignedValue != 0) {
            return false;
         }

         switch (unsignedValue) {
            case 0:
            case 128:
            case 192:
            case 224:
            case 240:
            case 248:
            case 252:
            case 254:
               notCompleteHit = true;
               break;
            case 255:
            default:
               return false;
         }
      }

      return true;
   }

   public static String numericStringToScopeSpec(String numericString) {
      if (isIpv6Address(numericString) && numericString.indexOf(37) != -1) {
         try {
            return numericString.substring(numericString.indexOf(37) + 1);
         } catch (Exception var2) {
         }
      }

      return null;
   }

   public static byte[] numericStringToByteArray(String numericString) {
      byte[] address;
      if (isIpv6Address(numericString)) {
         address = new byte[16];
         String expandedAddress = expandIPv6NumericString(numericString, true);
         if (expandedAddress.indexOf(37) != -1) {
            expandedAddress = expandedAddress.substring(0, expandedAddress.indexOf(37));
         }

         String[] octet = TextUtil.split(expandedAddress, ':');

         try {
            for (int i = 0; i < 16; i++) {
               String twoByteString = TextUtil.padZeros(octet[i / 2], 4);
               address[i++] = (byte)Integer.valueOf(twoByteString.substring(0, 2), 16).intValue();
               address[i] = (byte)Integer.valueOf(twoByteString.substring(2, 4), 16).intValue();
            }
         } catch (ArrayIndexOutOfBoundsException e) {
            return null;
         }
      } else {
         if (!isIpv4Address(numericString)) {
            if (!isIpv4MappedAddress(numericString)) {
               return null;
            }

            address = new byte[16];

            for (int i = 0; i < 10; i++) {
               address[i] = 0;
            }

            address[10] = -1;
            address[11] = -1;
            String[] quartet = TextUtil.split(numericString.substring(numericString.lastIndexOf(":") + 1), '.');

            try {
               int ipv6Index = 12;

               for (int i = 0; i < 4; i++) {
                  address[ipv6Index] = (byte)Integer.valueOf(quartet[i]).intValue();
                  ipv6Index++;
               }

               return address;
            } catch (ArrayIndexOutOfBoundsException e) {
               return null;
            }
         }

         address = new byte[4];
         String[] quartet = TextUtil.split(numericString, '.');

         try {
            for (int i = 0; i < 4; i++) {
               address[i] = (byte)Integer.valueOf(quartet[i]).intValue();
            }
         } catch (ArrayIndexOutOfBoundsException e) {
            return null;
         }
      }

      return address;
   }

   public static String removeScopeSpec(String hostAddress) {
      if (hostAddress.indexOf(37) != -1) {
         hostAddress = hostAddress.substring(0, hostAddress.indexOf(37));
      }

      return hostAddress;
   }

   public static InetAddress numericStringToInetAddress(String numericAddress) {
      byte[] addressAsBytes = numericStringToByteArray(numericAddress);
      if (addressAsBytes == null) {
         return null;
      }

      try {
         if (addressAsBytes.length == 4) {
            return InetAddress.getByAddress(addressAsBytes);
         }

         if (addressAsBytes.length == 16) {
            String scopeSpec = numericStringToScopeSpec(numericAddress);
            if (scopeSpec != null) {
               try {
                  return Inet6Address.getByAddress(null, addressAsBytes, Integer.parseInt(scopeSpec));
               } catch (NumberFormatException var4) {
                  return Inet6Address.getByAddress(null, addressAsBytes, NetworkInterface.getByName(scopeSpec));
               }
            } else {
               return InetAddress.getByAddress(addressAsBytes);
            }
         } else {
            throw new IllegalArgumentException("Invalid numeric address provided");
         }
      } catch (Exception var5) {
         return null;
      }
   }

   public static String expandIPv6NumericString(String ipv6Address, boolean expandCompletely) {
      boolean isIpv6Address = isIpv6Address(ipv6Address);
      boolean isIpv4MappedAddress = isIpv4MappedAddress(ipv6Address);
      if (!isIpv6Address && !isIpv4MappedAddress) {
         return ipv6Address;
      }

      int expansionIndex;
      if ((expansionIndex = ipv6Address.indexOf("::")) == -1) {
         return ipv6Address;
      }

      if (expansionIndex != ipv6Address.lastIndexOf("::")) {
         return ipv6Address;
      }

      String deviceId = null;
      if (ipv6Address.indexOf(37) != -1) {
         deviceId = ipv6Address.substring(ipv6Address.indexOf(37));
         ipv6Address = ipv6Address.substring(0, ipv6Address.indexOf(37));
      }

      int colonCount = 0;

      for (int i = 0; i < ipv6Address.length(); i++) {
         if (ipv6Address.charAt(i) == ':') {
            colonCount++;
         }
      }

      int ipv4MappedOffset = isIpv4MappedAddress ? 1 : 0;
      StringBuilder expansion = new StringBuilder(":");

      for (int i = 0; i < 8 - colonCount - ipv4MappedOffset; i++) {
         expansion.append("0:");
      }

      if (expansionIndex == 0) {
         expansion.insert(0, "0");
      }

      if (expansionIndex == ipv6Address.length() - 2) {
         expansion.append("0");
      }

      String result = TextUtil.replace(ipv6Address, "::", expansion.toString());
      if (expandCompletely) {
         String[] octets = TextUtil.split(result, ':');
         StringBuilder fullExpansion = new StringBuilder();

         for (int i = 0; i < 8 - ipv4MappedOffset; i++) {
            fullExpansion.append(TextUtil.padZeros(octets[i], 4));
            if (i != 7 - ipv4MappedOffset) {
               fullExpansion.append(":");
            }
         }

         if (deviceId != null) {
            fullExpansion.append(deviceId);
         }

         result = fullExpansion.toString();
      } else if (deviceId != null) {
         result = result + deviceId;
      }

      return result;
   }

   public static String getIPv4NetworkPrefix(String ipAddress, String subnetMask) {
      if (ipAddress == null) {
         throw new NullPointerException("IP Address parameter can not be null");
      }

      if (subnetMask == null) {
         throw new NullPointerException("Subnet Mask parameter can not be null");
      }

      if (!isIpv4Address(ipAddress)) {
         throw new IllegalStateException("IP Address is not valid");
      }

      if (!isIpv4SubnetMask(subnetMask)) {
         throw new IllegalStateException("Subnet Mask is not valid");
      }

      byte[] ipAddressBytes = numericStringToByteArray(ipAddress);
      byte[] subnetMaskBytes = numericStringToByteArray(subnetMask);
      StringBuilder networkAsString = new StringBuilder();
      if (ipAddressBytes == null) {
         throw new NullPointerException("Invalid IP Address parameter");
      }

      if (subnetMaskBytes == null) {
         throw new NullPointerException("Invalid Subnet Mask parameter");
      }

      for (int i = 0; i < 4; i++) {
         int quartet = ipAddressBytes[i] & subnetMaskBytes[i] & 0xFF;
         networkAsString.append(quartet);
         if (i != 3) {
            networkAsString.append('.');
         }
      }

      return networkAsString.toString();
   }

   public static int compare(Object thisObj, Object thatObj) {
      if (thisObj == null && thatObj == null) {
         return 0;
      }

      if (thisObj != null && thatObj == null) {
         return 1;
      }

      if (thisObj == null) {
         return -1;
      }

      if (thisObj == thatObj) {
         return 0;
      }

      String thisAddress = thisObj.toString();
      String thatAddress = thatObj.toString();
      boolean thisIsNumeric = isNumericAddr(thisAddress);
      boolean thatIsNumeric = isNumericAddr(thatAddress);
      if (thisIsNumeric && !thatIsNumeric) {
         return 1;
      }

      if (!thisIsNumeric && thatIsNumeric) {
         return -1;
      }

      if (thisIsNumeric) {
         if (thisAddress.equalsIgnoreCase(thatAddress)) {
            return 0;
         }

         boolean thisIsIPv4 = isIpv4Address(thisAddress);
         boolean thatIsIPv4 = isIpv4Address(thatAddress);
         if (thisIsIPv4 && !thatIsIPv4) {
            return -1;
         }

         if (!thisIsIPv4 && thatIsIPv4) {
            return 1;
         }

         if (thisIsIPv4) {
            String[] theseOctets = TextUtil.split(thisAddress, '.');
            String[] thoseOctets = TextUtil.split(thatAddress, '.');

            for (int i = 0; i < 4; i++) {
               int thisOctet = Integer.parseInt(theseOctets[i]);
               int thatOctet = Integer.parseInt(thoseOctets[i]);
               if (thisOctet < thatOctet) {
                  return -1;
               }

               if (thisOctet > thatOctet) {
                  return 1;
               }
            }
         } else {
            boolean thisIsIPv4Mapped = isIpv4MappedAddress(thisAddress);
            boolean thatIsIpv4Mapped = isIpv4MappedAddress(thatAddress);
            if (thisIsIPv4Mapped && !thatIsIpv4Mapped) {
               return -1;
            }

            if (!thisIsIPv4Mapped && thatIsIpv4Mapped) {
               return 1;
            }

            String[] theseOctets = TextUtil.split(expandIPv6NumericString(thisAddress, false), ':');
            String[] thoseOctets = TextUtil.split(expandIPv6NumericString(thatAddress, false), ':');
            if (thisIsIPv4Mapped) {
               for (int i = 0; i < 6; i++) {
                  int thisOctet = Integer.valueOf(theseOctets[i], 16);
                  int thatOctet = Integer.valueOf(thoseOctets[i], 16);
                  if (thisOctet < thatOctet) {
                     return -1;
                  }

                  if (thisOctet > thatOctet) {
                     return 1;
                  }
               }

               theseOctets = TextUtil.split(thisAddress.substring(thisAddress.lastIndexOf(":") + 1), '.');
               thoseOctets = TextUtil.split(thatAddress.substring(thatAddress.lastIndexOf(":") + 1), '.');

               for (int i = 0; i < 4; i++) {
                  int thisOctet = Integer.parseInt(theseOctets[i]);
                  int thatOctet = Integer.parseInt(thoseOctets[i]);
                  if (thisOctet < thatOctet) {
                     return -1;
                  }

                  if (thisOctet > thatOctet) {
                     return 1;
                  }
               }
            } else {
               for (int i = 0; i < 8; i++) {
                  int thisOctet = Integer.valueOf(theseOctets[i], 16);
                  int thatOctet = Integer.valueOf(thoseOctets[i], 16);
                  if (thisOctet < thatOctet) {
                     return -1;
                  }

                  if (thisOctet > thatOctet) {
                     return 1;
                  }
               }
            }
         }

         return 0;
      } else {
         return thisAddress.compareTo(thatAddress);
      }
   }

   public static InetAddress getLocalHost() {
      return getLocalHost(NIAGARA_USES_IPV6);
   }

   public static InetAddress getLocalHost(boolean isIPv6) {
      try {
         if (!isIPv6) {
            InetAddress localCopyLocalHost = localHost;
            if (localCopyLocalHost == null) {
               synchronized (localHostLock) {
                  localCopyLocalHost = localHost;
                  if (localCopyLocalHost == null) {
                     InetAddress localHostWorking = getBestLocalAddress(false);
                     if (localHostWorking == null) {
                        log.severe("No valid IPv4 addresses found, using IPv4 loopback as localhost");
                        localHostWorking = InetAddress.getByAddress(new byte[]{127, 0, 0, 1});
                     }

                     localCopyLocalHost = localHostWorking;
                     localHost = localHostWorking;
                  }
               }
            }

            return localCopyLocalHost;
         } else {
            InetAddress localCopyLocalHost6 = localHost6;
            if (localCopyLocalHost6 == null) {
               synchronized (localHostLock) {
                  localCopyLocalHost6 = localHost6;
                  if (localCopyLocalHost6 == null) {
                     InetAddress localHost6Working = getBestLocalAddress(true);
                     if (localHost6Working == null) {
                        log.severe("No valid IPv4 addresses found, using IPv4 loopback as localhost");
                        localHost6Working = InetAddress.getByAddress(new byte[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1});
                     }

                     localCopyLocalHost6 = localHost6Working;
                     localHost6 = localHost6Working;
                  }
               }
            }

            return localCopyLocalHost6;
         }
      } catch (Throwable t) {
         log.log(Level.SEVERE, "Failed to determine localhost address", t);
         throw new RuntimeException("getLocalHost", t);
      }
   }

   public static InetAddress getLocalHost(InetAddress hint) {
      try {
         synchronized (localHostLock) {
            if (getAllInterfaceAddresses().length == 0) {
               return getLocalHost(hint instanceof Inet6Address);
            }

            if (log.isLoggable(Level.FINEST)) {
               log.finest("Get local host with hint \"" + hint + "\"");
            }

            if (hint == null) {
               return getLocalHost();
            } else if (hint.isLoopbackAddress()) {
               return hint;
            } else if (addressedAdapters == 1 && hint instanceof Inet4Address) {
               return getLocalHost(false);
            } else {
               InterfaceNetworkSettings networkSettingsForHint = findInterfaceNetworkSettings(hint);
               if (networkSettingsForHint == null) {
                  log.finest("Failed to determine a subnet for hint, using default behavior");
                  return getLocalHost(hint instanceof Inet6Address);
               } else {
                  return networkSettingsForHint.getInetAddress();
               }
            }
         }
      } catch (Throwable t) {
         log.log(Level.SEVERE, "Failed to determine localhost address", t);
         throw new RuntimeException("getLocalHost", t);
      }
   }

   public static InterfaceNetworkSettings findInterfaceNetworkSettings(InetAddress hint) {
      if (hint == null) {
         return null;
      }

      synchronized (localHostLock) {
         InterfaceNetworkSettings interfaceNetworkSettings = localhostHints.get(hint);
         if (interfaceNetworkSettings == null) {
            for (InterfaceNetworkSettings currentSettings : getAllInterfaceAddresses()) {
               if (currentSettings.isSameSubnet(hint)) {
                  if (log.isLoggable(Level.FINEST)) {
                     log.finest(
                        "Hint \"" + hint + "\" is on same subnet as interface address \"" + currentSettings.getInetAddress() + "\", returning that value"
                     );
                  }

                  interfaceNetworkSettings = currentSettings;
                  break;
               }
            }

            localhostHints.put(hint, interfaceNetworkSettings);
         }

         return interfaceNetworkSettings;
      }
   }

   public static String getHostName() {
      try {
         String localCopyHostName = hostName;
         if (localCopyHostName == null) {
            synchronized (localHostLock) {
               localCopyHostName = hostName;
               if (localCopyHostName == null) {
                  String hostNameWorking = getLocalHostName();
                  if (hostNameWorking == null) {
                     log.severe("Failed to determine hostname, using empty string as local hostname");
                     hostNameWorking = "";
                  }

                  localCopyHostName = hostNameWorking;
                  hostName = hostNameWorking;
               }
            }
         }

         return localCopyHostName;
      } catch (Throwable t) {
         log.log(Level.SEVERE, "Failed to determine hostname", t);
         throw new RuntimeException("getHostName", t);
      }
   }

   public static void clearLocalHostCache() {
      synchronized (localHostLock) {
         localHost = null;
         localHost6 = null;
         hostName = null;
         localhostInterfaces = null;
         localhostHints.clear();
         addressedAdapters = 0;
      }
   }

   public static InterfaceNetworkSettings[] getLocalHostInterfaces() {
      return Arrays.copyOf(localhostInterfaces, localhostInterfaces.length);
   }

   private static InetAddress getBestLocalAddress(boolean getIPv6) {
      IPAddressUtil.FindBestLocalAddressAction findBestLocalAction = new IPAddressUtil.FindBestLocalAddressAction(getIPv6);
      return AccessController.doPrivileged(findBestLocalAction);
   }

   private static InterfaceNetworkSettings[] getAllInterfaceAddresses() {
      if (localhostInterfaces == null) {
         buildLocalhostInterfaces();
      }

      return localhostInterfaces;
   }

   private static void buildLocalhostInterfaces() {
      localhostHints.clear();
      addressedAdapters = 0;
      IPAddressUtil.FindAllLocalInterfaceSettings allLocalAddressesAction = new IPAddressUtil.FindAllLocalInterfaceSettings();
      localhostInterfaces = AccessController.doPrivileged(allLocalAddressesAction);

      for (InterfaceNetworkSettings settings : localhostInterfaces) {
         if (log.isLoggable(Level.FINEST)) {
            log.finest("Created local interface address: " + settings.getInetAddress());
         }
      }
   }

   private static String getLocalHostName() {
      IPAddressUtil.FindLocalHostNameAction findHostNameAction = new IPAddressUtil.FindLocalHostNameAction();
      return AccessController.doPrivileged(findHostNameAction);
   }

   private static boolean matchesPreferredAdapter(String interfaceName) {
      if (interfaceName == null) {
         return false;
      } else {
         return PREFERRED_ADAPTER_NAME == null ? false : PREFERRED_ADAPTER_NAME.equals(interfaceName);
      }
   }

   private static boolean matchesAdapterPattern(String interfaceName, Set<String> adapterPatterns) {
      if (interfaceName == null) {
         return false;
      }

      if (adapterPatterns == null) {
         return false;
      }

      for (String adapterPattern : adapterPatterns) {
         if (Pattern.matches(adapterPattern, interfaceName)) {
            return true;
         }
      }

      return false;
   }

   private static boolean isInteger(String s) {
      try {
         Integer.valueOf(s);
         return true;
      } catch (NumberFormatException e) {
         return false;
      }
   }

   private static boolean isValidNetworkInterfaceName(String interfaceName) {
      try {
         return NetworkInterface.getByName(interfaceName) != null;
      } catch (SocketException e) {
         return false;
      }
   }

   static {
      String virtualAdaptersProperty = AccessController.doPrivileged(
         () -> System.getProperty("niagara.virtual.network.adapters", "^docker[0-9]+;^virbr[0-9]+;^virt[0-9]+;^vnet[0-9]+")
      );
      VIRTUAL_ADAPTER_NAME_PATTERNS.addAll(Arrays.asList(virtualAdaptersProperty.split(";")));
      String excludedAdaptersProperty = AccessController.doPrivileged(() -> System.getProperty("niagara.excluded.network.adapters", ""));
      EXCLUDED_ADAPTER_NAME_PATTERNS.addAll(Arrays.asList(excludedAdaptersProperty.split(";")));
   }

   private static class FindAllLocalInterfaceSettings implements PrivilegedAction<InterfaceNetworkSettings[]> {
      private FindAllLocalInterfaceSettings() {
      }

      public InterfaceNetworkSettings[] run() {
         IPAddressUtil.addressedAdapters = 0;
         ArrayList<InterfaceNetworkSettings> preferredInterfaces = new ArrayList<>();
         ArrayList<InterfaceNetworkSettings> normalInterfaces = new ArrayList<>();
         ArrayList<InterfaceNetworkSettings> virtualInterfaces = new ArrayList<>();

         Enumeration<NetworkInterface> networkInterfaces;
         try {
            networkInterfaces = NetworkInterface.getNetworkInterfaces();
         } catch (SocketException se) {
            IPAddressUtil.log.log(Level.SEVERE, "Error during retrieving network interfaces", se);
            return null;
         }

         if (networkInterfaces == null) {
            return null;
         }

         while (networkInterfaces.hasMoreElements()) {
            NetworkInterface currentInterface = networkInterfaces.nextElement();

            try {
               if (currentInterface.isLoopback()) {
                  continue;
               }
            } catch (Exception var11) {
            }

            List<InterfaceAddress> interfaceAddresses = currentInterface.getInterfaceAddresses();
            if (interfaceAddresses.size() > 0) {
               IPAddressUtil.addressedAdapters++;

               for (InterfaceAddress interfaceAddress : interfaceAddresses) {
                  InterfaceNetworkSettings settings = InterfaceNetworkSettings.make(currentInterface, interfaceAddress);
                  if (IPAddressUtil.matchesPreferredAdapter(currentInterface.getName())) {
                     if (IPAddressUtil.log.isLoggable(Level.FINE)) {
                        IPAddressUtil.log.fine("Encountered preferred adapter '" + currentInterface + "', adding to front of interface list");
                     }

                     preferredInterfaces.add(settings);
                  } else if (IPAddressUtil.matchesAdapterPattern(currentInterface.getName(), IPAddressUtil.VIRTUAL_ADAPTER_NAME_PATTERNS)) {
                     if (IPAddressUtil.log.isLoggable(Level.FINE)) {
                        IPAddressUtil.log.fine("Encountered virtual adapter '" + currentInterface + "', adding to end of interface list");
                     }

                     virtualInterfaces.add(settings);
                  } else if (IPAddressUtil.matchesAdapterPattern(currentInterface.getName(), IPAddressUtil.EXCLUDED_ADAPTER_NAME_PATTERNS)) {
                     if (IPAddressUtil.log.isLoggable(Level.FINE)) {
                        IPAddressUtil.log.fine("Encountered excluded adapter '" + currentInterface + "', ignoring");
                     }
                  } else {
                     normalInterfaces.add(settings);
                  }

                  IPAddressUtil.localhostHints.put(interfaceAddress.getAddress(), settings);
               }
            }
         }

         ArrayList<InterfaceNetworkSettings> allInterfaces = new ArrayList<>();
         preferredInterfaces.sort(IPAddressUtil.INTERFACE_COMPARATOR);
         normalInterfaces.sort(IPAddressUtil.INTERFACE_COMPARATOR);
         virtualInterfaces.sort(IPAddressUtil.INTERFACE_COMPARATOR);
         allInterfaces.addAll(preferredInterfaces);
         allInterfaces.addAll(normalInterfaces);
         allInterfaces.addAll(virtualInterfaces);
         return allInterfaces.toArray(new InterfaceNetworkSettings[0]);
      }
   }

   private static class FindBestLocalAddressAction implements PrivilegedAction<InetAddress> {
      boolean targetIPv6;

      public FindBestLocalAddressAction(boolean targetIPv6) {
         this.targetIPv6 = targetIPv6;
      }

      public InetAddress run() {
         ArrayList<InetAddress> preferredAddresses = new ArrayList<>();
         ArrayList<InetAddress> normalAddresses = new ArrayList<>();
         ArrayList<InetAddress> virtualAddresses = new ArrayList<>();

         Enumeration<NetworkInterface> networkInterfaces;
         try {
            networkInterfaces = NetworkInterface.getNetworkInterfaces();
         } catch (SocketException se) {
            IPAddressUtil.log.log(Level.SEVERE, "Error during retrieving network interfaces", se);
            return null;
         }

         if (networkInterfaces == null) {
            return null;
         }

         while (networkInterfaces.hasMoreElements()) {
            NetworkInterface currentInterface = networkInterfaces.nextElement();

            try {
               if (currentInterface.isLoopback()) {
                  continue;
               }
            } catch (Exception var17) {
            }

            Enumeration<InetAddress> interfaceAddresses = currentInterface.getInetAddresses();

            while (interfaceAddresses.hasMoreElements()) {
               InetAddress currentInterfaceAddress = interfaceAddresses.nextElement();
               if (IPAddressUtil.matchesPreferredAdapter(currentInterface.getName())) {
                  if (IPAddressUtil.log.isLoggable(Level.FINE)) {
                     IPAddressUtil.log
                        .fine(
                           "Encountered preferred adapter '"
                              + currentInterface
                              + "' address '"
                              + currentInterfaceAddress
                              + "', adding to front of address list"
                        );
                  }

                  preferredAddresses.add(currentInterfaceAddress);
               } else if (IPAddressUtil.matchesAdapterPattern(currentInterface.getName(), IPAddressUtil.VIRTUAL_ADAPTER_NAME_PATTERNS)) {
                  if (IPAddressUtil.log.isLoggable(Level.FINE)) {
                     IPAddressUtil.log
                        .fine("Encountered virtual adapter '" + currentInterface + "' address '" + currentInterfaceAddress + "', adding to end of address list");
                  }

                  virtualAddresses.add(currentInterfaceAddress);
               } else if (IPAddressUtil.matchesAdapterPattern(currentInterface.getName(), IPAddressUtil.EXCLUDED_ADAPTER_NAME_PATTERNS)) {
                  if (IPAddressUtil.log.isLoggable(Level.FINE)) {
                     IPAddressUtil.log.fine("Encountered excluded adapter '" + currentInterface + "' address '" + currentInterfaceAddress + "', ignoring");
                  }
               } else {
                  normalAddresses.add(currentInterfaceAddress);
               }
            }
         }

         ArrayList<InetAddress> allInetAddresses = new ArrayList<>();
         preferredAddresses.sort(IPAddressUtil.INET_ADDRESS_COMPARATOR);
         normalAddresses.sort(IPAddressUtil.INET_ADDRESS_COMPARATOR);
         virtualAddresses.sort(IPAddressUtil.INET_ADDRESS_COMPARATOR);
         allInetAddresses.addAll(preferredAddresses);
         allInetAddresses.addAll(normalAddresses);
         allInetAddresses.addAll(virtualAddresses);
         InetAddress[] inetAddresses = allInetAddresses.toArray(new InetAddress[0]);
         if (IPAddressUtil.log.isLoggable(Level.FINER)) {
            IPAddressUtil.log.finer("FindBestLocalAddressAction considering address in the following order:");

            for (InetAddress inetAddress : inetAddresses) {
               String scope = "Unknown";
               if (inetAddress.isLinkLocalAddress()) {
                  scope = "Link Local";
               } else if (inetAddress.isSiteLocalAddress()) {
                  scope = "Site Local";
               } else if (!inetAddress.isLoopbackAddress()
                  && !inetAddress.isAnyLocalAddress()
                  && !inetAddress.isLinkLocalAddress()
                  && !inetAddress.isSiteLocalAddress()
                  && !inetAddress.isMulticastAddress()) {
                  scope = "Global";
               }

               IPAddressUtil.log.finer("Address: " + inetAddress + " Scope: " + scope);
            }
         }

         InetAddress linkLocal = null;
         InetAddress siteLocal = null;
         InetAddress global = null;

         for (InetAddress inetAddress : inetAddresses) {
            InetAddress currentAddress;
            if (this.targetIPv6 && inetAddress instanceof Inet6Address) {
               currentAddress = inetAddress;
            } else {
               if (this.targetIPv6 || !(inetAddress instanceof Inet4Address)) {
                  continue;
               }

               currentAddress = inetAddress;
            }

            if (linkLocal == null && currentAddress.isLinkLocalAddress()) {
               linkLocal = currentAddress;
            } else if (siteLocal == null && currentAddress.isSiteLocalAddress()) {
               siteLocal = currentAddress;
            } else if (!currentAddress.isLoopbackAddress()
               && !currentAddress.isAnyLocalAddress()
               && !currentAddress.isLinkLocalAddress()
               && !currentAddress.isSiteLocalAddress()
               && !currentAddress.isMulticastAddress()) {
               global = currentAddress;
               break;
            }
         }

         InetAddress currentAddress = null;
         if (global != null) {
            currentAddress = global;
         } else if (siteLocal != null) {
            currentAddress = siteLocal;
         } else if (linkLocal != null) {
            currentAddress = linkLocal;
         }

         return currentAddress;
      }
   }

   private static class FindLocalHostNameAction implements PrivilegedAction<String> {
      private FindLocalHostNameAction() {
      }

      public String run() {
         String computerName = IPAddressUtil.PlatformProviderHolder.PLATFORM_PROVIDER_INSTANCE.getComputerName();
         String computerDomain = IPAddressUtil.PlatformProviderHolder.PLATFORM_PROVIDER_INSTANCE.getComputerDomain(true);
         StringBuilder hostNameBuilder = new StringBuilder();
         if (computerName != null && !computerName.equals("") && !computerName.equals("localhost") && !computerName.equals("localhost.localdomain")) {
            hostNameBuilder.append(computerName);
            if (computerDomain != null) {
               hostNameBuilder.append(".").append(computerDomain);
            }

            return hostNameBuilder.toString();
         } else {
            return "";
         }
      }
   }

   private static final class PlatformProviderHolder {
      private static final IPlatformProvider PLATFORM_PROVIDER_INSTANCE = AccessController.doPrivileged(PlatformUtil::getPlatformProvider);
   }
}
