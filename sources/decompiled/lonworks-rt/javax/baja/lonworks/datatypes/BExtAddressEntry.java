package javax.baja.lonworks.datatypes;

import com.tridium.lonworks.datatypes.BLinkDescriptor;
import com.tridium.lonworks.util.LonByteArrayUtil;
import com.tridium.lonworks.util.NmUtil;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import javax.baja.lonworks.enums.BAddressType;
import javax.baja.lonworks.enums.BLonReceiveTimer;
import javax.baja.lonworks.enums.BLonRepeatTimer;
import javax.baja.lonworks.io.LonOutputStream;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BObject;
import javax.baja.sys.BSimple;
import javax.baja.sys.BajaRuntimeException;
import javax.baja.sys.Context;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public final class BExtAddressEntry extends BSimple implements BIAddressEntry {
   public static final BExtAddressEntry DEFAULT = new BExtAddressEntry();
   public static final Type TYPE = Sys.loadType(BExtAddressEntry.class);
   private static final int ADDRESS_ENTRY_LENGTH = 7;
   private byte[] addressEntry = new byte[]{0, 0, 3, 6, 0, 0, 0};
   private int descriptor = 0;

   public Type getType() {
      return TYPE;
   }

   public static BExtAddressEntry make(byte[] b) {
      return new BExtAddressEntry(b, 0);
   }

   @Deprecated
   public static BExtAddressEntry make(BAddressType addressType, int size, int groupOrSubnet, int memberOrNode, int descriptor) {
      return make(addressType, size, groupOrSubnet, memberOrNode, descriptor, 0);
   }

   @Deprecated
   public static BExtAddressEntry make(BAddressType addressType, int size, int groupOrSubnet, int memberOrNode, int descriptor, int domain) {
      return make(addressType, size, groupOrSubnet, memberOrNode, descriptor, domain, new BLinkDescriptor());
   }

   public static BExtAddressEntry make(
      BAddressType addressType, int size, int groupOrSubnet, int memberOrNode, int descriptor, int domain, BLinkDescriptor desc
   ) {
      return new BExtAddressEntry(
         addressType,
         size,
         groupOrSubnet,
         memberOrNode,
         descriptor,
         domain,
         desc.getRepeatTimer(),
         desc.getRetries(),
         desc.getReceiveTimer(),
         desc.getTransmitTimer()
      );
   }

   public static BExtAddressEntry make(
      BAddressType addressType,
      int size,
      int groupOrSubnet,
      int memberOrNode,
      int descriptor,
      int domain,
      BLonRepeatTimer rptTmr,
      int retries,
      BLonReceiveTimer rcvTmr,
      BLonRepeatTimer txTmr
   ) {
      return new BExtAddressEntry(addressType, size, groupOrSubnet, memberOrNode, descriptor, domain, rptTmr, retries, rcvTmr, txTmr);
   }

   @Deprecated
   public static BExtAddressEntry makeSubnetNodeEntry(BSubnetNode sn, int descriptor) {
      return make(BAddressType.subnetNode, 0, sn.getSubnetId(), sn.getNodeId(), descriptor);
   }

   public static BExtAddressEntry makeSubnetNodeEntry(BSubnetNode sn, int descriptor, int domainNdx, BLinkDescriptor desc) {
      return make(BAddressType.subnetNode, 0, sn.getSubnetId(), sn.getNodeId(), descriptor, domainNdx, desc);
   }

   public static BExtAddressEntry make(BIAddressEntry ie) {
      if (ie.isExtended()) {
         return (BExtAddressEntry)ie;
      } else {
         BAddressEntry ee = (BAddressEntry)ie;
         return new BExtAddressEntry(
            ee.getAddressType(),
            ee.getSize(),
            ee.getGroupOrSubnet(),
            ee.getMemberOrNode(),
            ee.getDescriptor(),
            ee.getDomain(),
            ee.getRepeatTimer(),
            ee.getRetries(),
            ee.getReceiveTimer(),
            ee.getTransmitTimer()
         );
      }
   }

   private BExtAddressEntry(byte[] a, int descriptor) {
      int len = a.length;
      if (len != 7) {
         throw new BajaRuntimeException("Invalid array length " + len + " in BExtAddressEntry make.");
      } else {
         this.addressEntry = new byte[len];
         System.arraycopy(a, 0, this.addressEntry, 0, len);
         this.descriptor = descriptor;
      }
   }

   private BExtAddressEntry() {
   }

   private BExtAddressEntry(
      BAddressType addressType,
      int size,
      int groupOrSubnet,
      int memberOrNode,
      int descriptor,
      int domain,
      BLonRepeatTimer rptTmr,
      int retries,
      BLonReceiveTimer rcvTmr,
      BLonRepeatTimer txTmr
   ) {
      this.setAddressType(addressType);
      this.descriptor = descriptor;
      if (this.addressEntry[0] != 0) {
         if (addressType.equals(BAddressType.group)) {
            this.setSize(size);
         }

         this.setGroupOrSubnet(groupOrSubnet);
         this.setMemberOrNode(memberOrNode);
         this.setDomain(domain);
         this.setRepeatTimer(rptTmr);
         this.setRetries(retries);
         this.setReceiveTimer(rcvTmr);
         this.setTransmitTimer(txTmr);
      }
   }

   @Override
   public BAddressType getAddressType() {
      if (this.addressEntry[0] == 0) {
         return this.addressEntry[1] == 0 ? BAddressType.none : BAddressType.turnaround;
      } else if (this.addressEntry[0] == 1) {
         return BAddressType.subnetNode;
      } else {
         return this.addressEntry[0] == 3 ? BAddressType.broadcast : BAddressType.group;
      }
   }

   @Override
   public boolean isGroupAddress() {
      return (this.addressEntry[0] & 128) != 0;
   }

   @Override
   public boolean isSubnetNodeAddress() {
      return this.addressEntry[0] == 1;
   }

   @Override
   public boolean isTurnAroundAddress() {
      return this.addressEntry[0] == 0 && this.addressEntry[1] != 0;
   }

   private void setAddressType(BAddressType addressType) {
      switch (addressType.getOrdinal()) {
         case 1:
            this.addressEntry[0] = (byte)(this.addressEntry[0] | 128);
            break;
         case 2:
            this.addressEntry[0] = 1;
            break;
         case 3:
            this.addressEntry[0] = 3;
            break;
         case 4:
            this.addressEntry[0] = 0;
            this.addressEntry[1] = 1;
            break;
         default:
            this.addressEntry[0] = 0;
            this.addressEntry[1] = 0;
      }
   }

   @Override
   public int getSize() {
      return this.addressEntry[0] & 127;
   }

   private void setSize(int size) {
      if (size > 64) {
         size = 0;
      }

      this.addressEntry[0] = (byte)(size & 127 | this.addressEntry[0] & 128);
   }

   @Override
   public int getGroupOrSubnet() {
      return this.addressEntry[4] & 0xFF;
   }

   private void setGroupOrSubnet(int groupOrSubnet) {
      this.addressEntry[4] = (byte)groupOrSubnet;
   }

   @Override
   public int getMemberOrNode() {
      return this.addressEntry[1] & 127;
   }

   private void setMemberOrNode(int memberOrNode) {
      this.addressEntry[1] = (byte)(memberOrNode & 127 | this.addressEntry[1] & 128);
   }

   @Override
   public int getDomain() {
      return this.addressEntry[5] << 8 & this.addressEntry[6];
   }

   private void setDomain(int dom) {
      this.addressEntry[5] = (byte)(dom >> 8 & 0xFF);
      this.addressEntry[6] = (byte)(dom & 0xFF);
   }

   @Override
   public BLonRepeatTimer getRepeatTimer() {
      return BLonRepeatTimer.make((this.addressEntry[2] & 240) >> 4);
   }

   private void setRepeatTimer(BLonRepeatTimer rptTmr) {
      this.addressEntry[2] = (byte)(rptTmr.getOrdinal() << 4 & 240 | this.addressEntry[2] & 15);
   }

   @Override
   public int getRetries() {
      return this.addressEntry[2] & 15;
   }

   private void setRetries(int retries) {
      this.addressEntry[2] = (byte)(retries & 15 | this.addressEntry[2] & 240);
   }

   @Override
   public BLonReceiveTimer getReceiveTimer() {
      return BLonReceiveTimer.make((this.addressEntry[3] & 240) >> 4);
   }

   private void setReceiveTimer(BLonReceiveTimer rcvTmr) {
      this.addressEntry[3] = (byte)(rcvTmr.getOrdinal() << 4 & 240 | this.addressEntry[3] & 15);
   }

   @Override
   public BLonRepeatTimer getTransmitTimer() {
      return BLonRepeatTimer.make(this.addressEntry[3] & 15);
   }

   private void setTransmitTimer(BLonRepeatTimer txTmr) {
      this.addressEntry[3] = (byte)(txTmr.getOrdinal() & 15 | this.addressEntry[3] & 240);
   }

   @Override
   public int getDescriptor() {
      return this.descriptor;
   }

   @Override
   public boolean isExtended() {
      return true;
   }

   @Override
   public BSubnetNode getSubnetNodeAddress() {
      return BSubnetNode.make(this.getGroupOrSubnet(), this.getMemberOrNode());
   }

   public byte[] getRawAddress() {
      return this.addressEntry;
   }

   @Override
   public boolean equals(Object obj) {
      if (!(obj instanceof BExtAddressEntry)) {
         return false;
      } else {
         BExtAddressEntry comp = (BExtAddressEntry)obj;
         if (this.descriptor != comp.descriptor) {
            return false;
         } else if (this.addressEntry.length != comp.addressEntry.length) {
            return false;
         } else {
            for (int i = 0; i < 7; i++) {
               if (this.addressEntry[i] != comp.addressEntry[i]) {
                  return false;
               }
            }

            return true;
         }
      }
   }

   @Override
   public boolean isSameAddress(BIAddressEntry iae) {
      return this.getAddressType() == iae.getAddressType()
         && this.isGroupAddress() == iae.isGroupAddress()
         && this.isSubnetNodeAddress() == iae.isSubnetNodeAddress()
         && this.isTurnAroundAddress() == iae.isTurnAroundAddress()
         && this.getSize() == iae.getSize()
         && this.getGroupOrSubnet() == iae.getGroupOrSubnet()
         && this.getMemberOrNode() == iae.getMemberOrNode()
         && this.getDescriptor() == iae.getDescriptor()
         && this.getDomain() == iae.getDomain();
   }

   @Override
   public String toString(Context context) {
      StringBuilder sb = new StringBuilder();
      if (this.addressEntry[0] == 0) {
         if (this.addressEntry[1] == 0) {
            return "Not In Use";
         }

         sb.append("Turnaround");
      } else if (this.addressEntry[0] == 1) {
         sb.append("Subnet ").append(this.getGroupOrSubnet());
         sb.append(" Node ").append(this.getMemberOrNode());
      } else if (this.addressEntry[0] == 3) {
         sb.append("Broadcast on subnet ").append(this.getGroupOrSubnet());
      } else {
         sb.append("Group# ").append(this.getGroupOrSubnet());
         sb.append(" Member# ").append(this.getMemberOrNode());
         sb.append(" size ").append(this.getSize());
      }

      sb.append(" Domain ").append(this.getDomain());
      sb.append(" ").append(NmUtil.getLinkType(this.descriptor));
      return sb.toString();
   }

   public void encode(DataOutput out) throws IOException {
      out.writeInt(this.descriptor);
      out.writeInt(7);
      out.write(this.addressEntry);
   }

   public BObject decode(DataInput in) throws IOException {
      int descriptor = in.readInt();
      int aLen = in.readInt();
      byte[] a = new byte[aLen];
      in.readFully(a, 0, aLen);
      return new BExtAddressEntry(a, descriptor);
   }

   @Override
   public String encodeToString() throws IOException {
      return this.descriptor + "/" + this.addressEntry.length + "/" + LonByteArrayUtil.toString(this.addressEntry);
   }

   public BObject decodeFromString(String s) throws IOException {
      try {
         int pos = s.indexOf("/");
         int descr = Integer.parseInt(s.substring(0, pos));
         int pos2 = s.indexOf("/", pos + 1);
         int len = Integer.parseInt(s.substring(pos + 1, pos2));
         byte[] a = LonByteArrayUtil.getBytes(s.substring(pos2 + 1), len);
         return new BExtAddressEntry(a, descr);
      } catch (Throwable var7) {
         System.out.println("FORMATE ERROR {" + s + "}\n" + var7);
         return DEFAULT;
      }
   }

   public void toOutputStream(LonOutputStream out) {
      out.writeByteArray(this.addressEntry);
   }
}
