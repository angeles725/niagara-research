package javax.baja.lonworks.datatypes;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BObject;
import javax.baja.sys.BSimple;
import javax.baja.sys.Context;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public final class BBroadcast extends BSimple implements LonAddress {
   public static final BBroadcast domain0 = new BBroadcast(0);
   public static final BBroadcast domain1 = new BBroadcast(1);
   public static final BBroadcast DEFAULT = domain0;
   public static final Type TYPE = Sys.loadType(BBroadcast.class);
   private int domainNdx;

   public Type getType() {
      return TYPE;
   }

   public static BBroadcast make(int d) {
      return d == 0 ? domain0 : domain1;
   }

   private BBroadcast(int d) {
      this.domainNdx = d;
   }

   public int getDomainIndex() {
      return this.domainNdx;
   }

   @Override
   public boolean equals(Object obj) {
      return !(obj instanceof BBroadcast) ? false : this.domainNdx == ((BBroadcast)obj).domainNdx;
   }

   public String toString(Context context) {
      return "broadcast domain" + this.domainNdx;
   }

   @Override
   public int hashCode() {
      return 50331648 | this.domainNdx;
   }

   public void encode(DataOutput out) throws IOException {
      out.writeInt(this.domainNdx);
   }

   public BObject decode(DataInput in) throws IOException {
      return make(in.readInt());
   }

   public String encodeToString() throws IOException {
      return this.toString(null);
   }

   public BObject decodeFromString(String s) throws IOException {
      return make(Integer.decode(s.substring(s.length() - 1)));
   }

   @Override
   public int getAddressType() {
      return 3;
   }
}
