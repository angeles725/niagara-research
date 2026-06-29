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
public final class BLocal extends BSimple implements LonAddress {
   public static final BLocal local = new BLocal();
   public static final BLocal DEFAULT = local;
   public static final Type TYPE = Sys.loadType(BLocal.class);

   public Type getType() {
      return TYPE;
   }

   public static BLocal make() {
      return local;
   }

   @Override
   public boolean equals(Object obj) {
      return obj instanceof BLocal;
   }

   @Override
   public int hashCode() {
      return 2130706432;
   }

   public String toString(Context context) {
      return "local device";
   }

   public void encode(DataOutput out) throws IOException {
   }

   public BObject decode(DataInput in) throws IOException {
      return local;
   }

   public String encodeToString() throws IOException {
      return this.toString(null);
   }

   public BObject decodeFromString(String s) throws IOException {
      return local;
   }

   @Override
   public int getAddressType() {
      return 127;
   }
}
