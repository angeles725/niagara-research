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
public final class BImplicit extends BSimple implements LonAddress {
   public static final BImplicit DEFAULT = new BImplicit(0);
   public static final Type TYPE = Sys.loadType(BImplicit.class);
   private int tag;

   public Type getType() {
      return TYPE;
   }

   public static BImplicit make(int tag) {
      return new BImplicit(tag);
   }

   public BImplicit makeFrom(int tag) {
      return this.tag == tag ? this : new BImplicit(tag);
   }

   private BImplicit(int t) {
      this.tag = t;
   }

   public int getTag() {
      return this.tag;
   }

   @Override
   public int hashCode() {
      return 2113929216 | this.tag;
   }

   @Override
   public boolean equals(Object obj) {
      return !(obj instanceof BImplicit) ? false : ((BImplicit)obj).tag == this.tag;
   }

   public String toString(Context context) {
      return "Implicit " + this.tag;
   }

   public void encode(DataOutput out) throws IOException {
      out.writeInt(this.tag);
   }

   public BObject decode(DataInput in) throws IOException {
      return this.makeFrom(in.readInt());
   }

   public String encodeToString() throws IOException {
      return Integer.toString(this.tag);
   }

   public BObject decodeFromString(String s) throws IOException {
      int t = Integer.parseInt(s);
      return t == this.tag ? this : make(t);
   }

   @Override
   public int getAddressType() {
      return 126;
   }
}
