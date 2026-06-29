package javax.baja.bacnet.datatypes;

import java.util.StringTokenizer;
import javax.baja.bacnet.BacnetConst;
import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.io.AsnInput;
import javax.baja.bacnet.io.AsnOutput;
import javax.baja.naming.SlotPath;
import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.util.TextUtil;
import javax.baja.sys.BFacets;
import javax.baja.sys.BStruct;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "addressType",
      type = "int",
      defaultValue = "0",
      flags = 4
   ), @NiagaraProperty(
      name = "networkNumber",
      type = "int",
      defaultValue = "0",
      facets = {@Facet("BFacets.makeInt(0, 65535)")}
   ), @NiagaraProperty(
      name = "macAddress",
      type = "BBacnetOctetString",
      defaultValue = "BBacnetOctetString.DEFAULT"
   )})
public final class BBacnetAddress extends BStruct implements BIBacnetDataType {
   public static final Property addressType = newProperty(4, 0, null);
   public static final Property networkNumber = newProperty(0, 0, BFacets.makeInt(0, 65535));
   public static final Property macAddress = newProperty(0, BBacnetOctetString.DEFAULT, null);
   public static final Type TYPE = Sys.loadType(BBacnetAddress.class);
   public static final int LOCAL_NETWORK = 0;
   public static final int BROADCAST_NETWORK = 65535;
   public static final int NETWORK_NUMBER_MASK = 65535;
   public static final int MAC_TYPE_UNKNOWN = 0;
   public static final int MAC_TYPE_ETHERNET = 1;
   public static final int MAC_TYPE_IP = 2;
   public static final int MAC_TYPE_MSTP = 3;
   public static final int MAC_TYPE_SC = 4;
   public static final BBacnetAddress GLOBAL_BROADCAST_ADDRESS = new BBacnetAddress(65535, (BBacnetOctetString)null);
   public static final BBacnetAddress LOCAL_BROADCAST_ADDRESS = new BBacnetAddress(0, (BBacnetOctetString)null);
   public static final BBacnetAddress DEFAULT = LOCAL_BROADCAST_ADDRESS;

   public int getAddressType() {
      return this.getInt(addressType);
   }

   public void setAddressType(int v) {
      this.setInt(addressType, v, null);
   }

   public int getNetworkNumber() {
      return this.getInt(networkNumber);
   }

   public void setNetworkNumber(int v) {
      this.setInt(networkNumber, v, null);
   }

   public BBacnetOctetString getMacAddress() {
      return (BBacnetOctetString)this.get(macAddress);
   }

   public void setMacAddress(BBacnetOctetString v) {
      this.set(macAddress, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BBacnetAddress() {
   }

   public BBacnetAddress(int networkNumber, byte[] macAddress) {
      this.setNetworkNumber(networkNumber);
      this.setMacAddress(BBacnetOctetString.make(macAddress));
   }

   public BBacnetAddress(int networkNumber, BBacnetOctetString macAddress) {
      this.setNetworkNumber(networkNumber);
      if (macAddress != null) {
         this.setMacAddress(macAddress);
      }
   }

   public void setMac(byte[] mac, Context cx) {
      this.set(macAddress, BBacnetOctetString.make(mac), cx);
   }

   public int hash() {
      return 31 * this.getNetworkNumber() + this.getMacAddress().hashCode();
   }

   public String toString(Context context) {
      StringBuilder sb = new StringBuilder();
      boolean nameContext = false;
      if (context == null || !(nameContext = context.equals(BacnetConst.nameContext)) && !context.equals(BacnetConst.deviceRegistryContext)) {
         sb.append(this.getNetworkNumber()).append(':');
         byte[] b = this.getMacAddress().getBytes();
         if (b == null) {
            sb.append("null");
         } else {
            switch (this.getAddressType()) {
               case 0:
               case 1:
               case 3:
               case 4:
                  for (int i = 0; i < b.length; i++) {
                     sb.append(TextUtil.byteToHexString(b[i])).append(' ');
                  }

                  sb.setLength(sb.length() - 1);
                  break;
               case 2:
                  sb.append(b[0] & 255).append('.');
                  sb.append(b[1] & 255).append('.');
                  sb.append(b[2] & 255).append('.');
                  sb.append(b[3] & 255).append(':');
                  sb.append((b[4] & 255) << 8 | b[5] & 255);
                  break;
               default:
                  sb.append(this.getMacAddress().toString(context));
            }
         }

         return sb.toString();
      } else {
         sb.append('_').append(this.getNetworkNumber()).append('_');
         sb.append(this.getMacAddress().toString(context));
         String str = sb.toString();
         return nameContext ? SlotPath.escape(str) : str;
      }
   }

   @Override
   public void writeAsn(AsnOutput out) {
      out.writeUnsignedInteger(this.getNetworkNumber());
      byte[] b = this.getMacAddress().getBytes();
      if (b == null) {
         b = new byte[0];
      }

      out.writeOctetString(b);
   }

   @Override
   public void readAsn(AsnInput in) throws AsnException {
      int networkNumber = in.readUnsignedInt();
      BBacnetOctetString macAddress = BBacnetOctetString.make(in.readOctetString());
      this.setInt(BBacnetAddress.networkNumber, networkNumber, noWrite);
      this.set(BBacnetAddress.macAddress, macAddress, noWrite);
   }

   public boolean equals(int networkNumber, byte[] macAddress) {
      if (networkNumber != this.getNetworkNumber()) {
         return false;
      } else {
         byte[] mac = this.getMacAddress().getBytes();
         if (macAddress == null && mac != null) {
            return false;
         } else if (macAddress != null && mac == null) {
            return false;
         } else if (macAddress == null && mac == null) {
            return true;
         } else {
            int len = mac.length;
            if (macAddress.length != len) {
               return false;
            } else {
               for (int i = 0; i < len; i++) {
                  if (macAddress[i] != mac[i]) {
                     return false;
                  }
               }

               return true;
            }
         }
      }
   }

   public boolean macEquals(byte[] macAddress) {
      byte[] mac = this.getMacAddress().getBytes();
      if (macAddress == null && mac != null) {
         return false;
      } else if (macAddress != null && mac == null) {
         return false;
      } else if (macAddress == null && mac == null) {
         return true;
      } else {
         int len = mac.length;
         if (macAddress.length != len) {
            return false;
         } else {
            for (int i = 0; i < len; i++) {
               if (macAddress[i] != mac[i]) {
                  return false;
               }
            }

            return true;
         }
      }
   }

   public static final String bytesToString(int type, byte[] mac) {
      if (mac == null) {
         return "null";
      } else {
         StringBuilder sb = new StringBuilder();
         switch (type) {
            case 1:
               if (mac.length != 6) {
                  throw new IllegalArgumentException("Invalid Ethernet MAC address!");
               }

               for (int i = 0; i < 6; i++) {
                  sb.append(TextUtil.byteToHexString(mac[i]).toUpperCase()).append(':');
               }

               sb.setLength(sb.length() - 1);
               break;
            case 2:
               sb.append(255 & mac[0]).append('.').append(255 & mac[1]).append('.').append(255 & mac[2]).append('.').append(255 & mac[3]);
               if (mac.length > 5) {
                  sb.append(':');
                  int port = (255 & mac[4]) << 8;
                  port |= 255 & mac[5];
                  sb.append("0x").append(Integer.toHexString(port).toUpperCase());
               }
               break;
            case 3:
            default:
               if (mac.length == 1) {
                  int i = mac[0] & 255;
                  sb.append(i);
               } else {
                  for (int i = 0; i < mac.length; i++) {
                     sb.append(TextUtil.byteToHexString(mac[i]).toUpperCase()).append(' ');
                  }

                  sb.setLength(sb.length() - 1);
               }
               break;
            case 4:
               if (mac.length != 6) {
                  throw new IllegalArgumentException("Invalid BACnet/SC MAC address!");
               }

               for (int i = 0; i < mac.length; i++) {
                  sb.append(TextUtil.byteToHexString(mac[i]).toUpperCase()).append(' ');
               }

               sb.setLength(sb.length() - 1);
         }

         return sb.toString();
      }
   }

   public static final byte[] stringToBytes(int type, int len, String s) {
      if (s != null && s.length() != 0 && !s.equalsIgnoreCase("null")) {
         byte[] b;
         switch (type) {
            case 1:
               StringTokenizer stx = new StringTokenizer(s, " :");
               if (stx.countTokens() != 6) {
                  throw new IllegalArgumentException("Invalid Ethernet MAC Address!");
               }

               b = new byte[6];

               for (int i = 0; i < 6; i++) {
                  b[i] = (byte)Integer.parseInt(stx.nextToken(), 16);
               }
               break;
            case 2:
               StringTokenizer stx = new StringTokenizer(s, ".: ");
               if (stx.countTokens() < len) {
                  throw new IllegalArgumentException("Invalid BACnet/IP MAC Address!");
               }

               switch (len) {
                  case 4:
                     b = new byte[4];

                     for (int i = 0; i < 4; i++) {
                        b[i] = (byte)Integer.decode(stx.nextToken()).intValue();
                     }

                     return b;
                  case 5:
                     b = new byte[6];

                     for (int i = 0; i < 4; i++) {
                        b[i] = (byte)Integer.decode(stx.nextToken()).intValue();
                     }

                     int port = Integer.decode(stx.nextToken());
                     b[4] = (byte)(port >> 8 & 0xFF);
                     b[5] = (byte)(port & 0xFF);
                     return b;
                  default:
                     throw new IllegalArgumentException("Invalid length for conversion of BACnet/IP MAC address!");
               }
            case 3:
            default:
               StringTokenizer st = new StringTokenizer(s, ": ");
               int maclen = st.countTokens();
               b = new byte[maclen];
               if (maclen == 1) {
                  b[0] = Integer.decode(st.nextToken()).byteValue();
               } else {
                  for (int i = 0; i < b.length; i++) {
                     b[i] = (byte)Integer.parseInt(st.nextToken(), 16);
                  }
               }
               break;
            case 4:
               StringTokenizer stx = new StringTokenizer(s, " ");
               if (stx.countTokens() != 6) {
                  throw new IllegalArgumentException("Invalid BACnet/SC MAC Address!");
               }

               b = new byte[6];

               for (int i = 0; i < 6; i++) {
                  b[i] = (byte)Integer.parseInt(stx.nextToken(), 16);
               }
         }

         return b;
      } else {
         return null;
      }
   }
}
