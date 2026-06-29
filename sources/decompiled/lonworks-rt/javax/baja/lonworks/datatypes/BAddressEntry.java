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
import javax.baja.lonworks.io.LonInputStream;
import javax.baja.lonworks.io.LonOutputStream;
import javax.baja.lonworks.londata.BILonNetworkSimple;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BObject;
import javax.baja.sys.BSimple;
import javax.baja.sys.BajaRuntimeException;
import javax.baja.sys.Context;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public final class BAddressEntry extends BSimple implements BILonNetworkSimple, BIAddressEntry {
   public static final BAddressEntry DEFAULT = new BAddressEntry();
   public static final Type TYPE = Sys.loadType(BAddressEntry.class);
   private static final int ADDRESS_ENTRY_LENGTH = 5;
   private byte[] addressEntry = new byte[]{0, 0, 3, 6, 0};
   private int descriptor = 0;

   public Type getType() {
      return TYPE;
   }

   public static BAddressEntry make(byte[] b) {
      return new BAddressEntry(b, 0);
   }

   @Deprecated
   public static BAddressEntry make(BAddressType addressType, int size, int groupOrSubnet, int memberOrNode, int descriptor) {
      return make(addressType, size, groupOrSubnet, memberOrNode, descriptor, 0);
   }

   @Deprecated
   public static BAddressEntry make(BAddressType addressType, int size, int groupOrSubnet, int memberOrNode, int descriptor, int domain) {
      return make(addressType, size, groupOrSubnet, memberOrNode, descriptor, domain, new BLinkDescriptor());
   }

   public static BAddressEntry make(BAddressType addressType, int size, int groupOrSubnet, int memberOrNode, int descriptor, int domain, BLinkDescriptor desc) {
      return new BAddressEntry(
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

   public static BAddressEntry make(
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
      return new BAddressEntry(addressType, size, groupOrSubnet, memberOrNode, descriptor, domain, rptTmr, retries, rcvTmr, txTmr);
   }

   @Deprecated
   public static BAddressEntry makeSubnetNodeEntry(BSubnetNode sn, int descriptor) {
      return makeSubnetNodeEntry(sn, descriptor, 0);
   }

   @Deprecated
   public static BAddressEntry makeSubnetNodeEntry(BSubnetNode sn, int descriptor, int domainNdx) {
      return make(BAddressType.subnetNode, 0, sn.getSubnetId(), sn.getNodeId(), descriptor, domainNdx);
   }

   public static BAddressEntry makeSubnetNodeEntry(BSubnetNode sn, int descriptor, int domainNdx, BLinkDescriptor desc) {
      return make(BAddressType.subnetNode, 0, sn.getSubnetId(), sn.getNodeId(), descriptor, domainNdx, desc);
   }

   public static BAddressEntry make(BIAddressEntry ie) {
      if (!ie.isExtended()) {
         return (BAddressEntry)ie;
      } else {
         BExtAddressEntry ee = (BExtAddressEntry)ie;
         return new BAddressEntry(
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

   private BAddressEntry(byte[] a, int descriptor) {
      int len = a.length;
      if (len != 5) {
         throw new BajaRuntimeException("Invalid array length " + len + " in BAddressEntry make.");
      } else {
         this.addressEntry = new byte[len];
         System.arraycopy(a, 0, this.addressEntry, 0, len);
         this.descriptor = descriptor;
      }
   }

   private BAddressEntry() {
   }

   private BAddressEntry(
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
      this.setRepeatTimer(rptTmr);
      this.setRetries(retries);
      this.setReceiveTimer(rcvTmr);
      this.setTransmitTimer(txTmr);
      if (this.addressEntry[0] != 0) {
         if (addressType.equals(BAddressType.group)) {
            this.setSize(size);
         }

         this.setGroupOrSubnet(groupOrSubnet);
         this.setMemberOrNode(memberOrNode);
         this.setDomain(domain);
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
      return this.addressEntry[1] >> 7 & 1;
   }

   private void setDomain(int dom) {
      if (dom >= 0 && dom <= 1) {
         this.addressEntry[1] = (byte)(dom << 7 | this.addressEntry[1] & 127);
      } else {
         throw new BajaRuntimeException("Invalid domain for BAddressEntry - must be 0/1.");
      }
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
   public BSubnetNode getSubnetNodeAddress() {
      return BSubnetNode.make(this.getGroupOrSubnet(), this.getMemberOrNode());
   }

   @Override
   public boolean isExtended() {
      return false;
   }

   public byte[] getRawAddress() {
      return this.addressEntry;
   }

   @Override
   public boolean equals(Object obj) {
      if (!(obj instanceof BAddressEntry)) {
         return false;
      } else {
         BAddressEntry comp = (BAddressEntry)obj;
         if (this.descriptor != comp.descriptor) {
            return false;
         } else {
            for (int i = 0; i < 5; i++) {
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
      out.write(this.addressEntry);
   }

   public BObject decode(DataInput in) throws IOException {
      byte[] pId = new byte[5];
      this.descriptor = in.readInt();
      in.readFully(pId, 0, 5);
      return new BAddressEntry(pId, this.descriptor);
   }

   @Override
   public String encodeToString() throws IOException {
      return this.descriptor + "/" + LonByteArrayUtil.toString(this.addressEntry);
   }

   public BObject decodeFromString(String s) throws IOException {
      try {
         int pos = s.indexOf("/");
         int descr = Integer.parseInt(s.substring(0, pos));
         byte[] a = LonByteArrayUtil.getBytes(s.substring(pos + 1), 5);
         return new BAddressEntry(a, descr);
      } catch (Throwable var5) {
         return DEFAULT;
      }
   }

   @Override
   public void toOutputStream(LonOutputStream out) {
      out.writeByteArray(this.addressEntry);
   }

   @Override
   public BILonNetworkSimple fromInputStream(LonInputStream in) {
      return new BAddressEntry(in);
   }

   private BAddressEntry(LonInputStream in) {
      for (int i = 0; i < 5; i++) {
         this.addressEntry[i] = (byte)in.read();
      }
   }

   @Override
   public int getNetLength() {
      return 5;
   }
}
