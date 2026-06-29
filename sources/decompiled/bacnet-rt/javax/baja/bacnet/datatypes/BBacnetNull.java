package javax.baja.bacnet.datatypes;

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
public final class BBacnetNull extends BSimple {
   public static final BBacnetNull DEFAULT = new BBacnetNull();
   public static final String NULL_STR = "NULL";
   public static final Type TYPE = Sys.loadType(BBacnetNull.class);

   private BBacnetNull() {
   }

   public boolean isNull() {
      return true;
   }

   public boolean equals(Object obj) {
      return obj instanceof BBacnetNull;
   }

   public void encode(DataOutput out) throws IOException {
      out.writeByte(0);
   }

   public BObject decode(DataInput in) throws IOException {
      in.readByte();
      return DEFAULT;
   }

   public String encodeToString() throws IOException {
      return "NULL";
   }

   public BObject decodeFromString(String s) throws IOException {
      if (!s.equals("NULL")) {
         throw new IOException(s);
      } else {
         return DEFAULT;
      }
   }

   public String toString(Context context) {
      return "NULL";
   }

   public int hashCode() {
      return 0;
   }

   public Type getType() {
      return TYPE;
   }
}
