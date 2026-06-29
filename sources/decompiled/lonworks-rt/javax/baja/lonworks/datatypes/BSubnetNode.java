package javax.baja.lonworks.datatypes;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.util.IntHashMap;
import javax.baja.sys.BObject;
import javax.baja.sys.BSimple;
import javax.baja.sys.BajaRuntimeException;
import javax.baja.sys.Context;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.util.Lexicon;

@NiagaraType
public final class BSubnetNode extends BSimple implements LonAddress {
   public static final BSubnetNode DEFAULT = new BSubnetNode(0, 0);
   public static final Type TYPE = Sys.loadType(BSubnetNode.class);
   static IntHashMap cache = new IntHashMap();
   private int subnetId;
   private int nodeId;
   private static Lexicon LON_LEX = Lexicon.make("lonworks");

   public Type getType() {
      return TYPE;
   }

   public static BSubnetNode make(int subnetId, int nodeId) {
      synchronized (cache) {
         int hash = subnetId << 8 | nodeId;
         BSubnetNode sn = (BSubnetNode)cache.get(hash);
         if (sn == null) {
            sn = new BSubnetNode(subnetId, nodeId);
            cache.put(hash, sn);
         }

         return sn;
      }
   }

   public BSubnetNode makeFrom(int subnetId, int nodeId) {
      return make(subnetId, nodeId);
   }

   private BSubnetNode(int subnetId, int nodeId) {
      if (subnetId >= 0 && subnetId <= 255 && nodeId >= 0 && nodeId <= 127) {
         this.subnetId = subnetId;
         this.nodeId = nodeId;
      } else {
         throw new BajaRuntimeException(LON_LEX.getText("SubnetNode.invalidInput.message", new Object[]{subnetId, nodeId}));
      }
   }

   public int getSubnetId() {
      return this.subnetId;
   }

   public int getNodeId() {
      return this.nodeId;
   }

   @Override
   public int hashCode() {
      return 16777216 | this.subnetId << 8 | this.nodeId;
   }

   @Override
   public boolean equals(Object obj) {
      if (!(obj instanceof BSubnetNode)) {
         return false;
      } else {
         BSubnetNode comp = (BSubnetNode)obj;
         return this.compare(comp.subnetId, comp.nodeId);
      }
   }

   private boolean compare(int subnet, int node) {
      return this.subnetId == subnet && this.nodeId == node;
   }

   public String toString(Context context) {
      return this.subnetId + "/" + this.nodeId;
   }

   public void encode(DataOutput out) throws IOException {
      out.writeInt(this.subnetId);
      out.writeInt(this.nodeId);
   }

   public BObject decode(DataInput in) throws IOException {
      return this.makeFrom(in.readInt(), in.readInt());
   }

   public String encodeToString() throws IOException {
      return this.subnetId + "/" + this.nodeId;
   }

   public BObject decodeFromString(String s) throws IOException {
      int pos = s.indexOf("/");
      int subnet = Integer.parseInt(s.substring(0, pos));
      int node = Integer.parseInt(s.substring(pos + 1));
      return this.makeFrom(subnet, node);
   }

   @Override
   public int getAddressType() {
      return 1;
   }
}
