package javax.baja.lonworks.datatypes;

import com.tridium.lonworks.util.LonByteArrayUtil;
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
public final class BNeuronId extends BSimple implements LonAddress {
   private static final byte[] defaultId = new byte[]{0, 0, 0, 0, 0, 0};
   public static final BNeuronId DEFAULT = new BNeuronId(defaultId);
   public static final Type TYPE = Sys.loadType(BNeuronId.class);
   public static final int NEURON_ID_LENGTH = 6;
   private byte[] neuronId;

   public Type getType() {
      return TYPE;
   }

   public static BNeuronId make(byte[] b) {
      return new BNeuronId(b);
   }

   private BNeuronId(byte[] b) {
      if (b.length != 6) {
         throw new IllegalArgumentException("Invalid array length in BNeuronId constructor. " + LonByteArrayUtil.toString(b));
      } else {
         this.neuronId = b;
      }
   }

   public byte[] getByteArray() {
      byte[] a = new byte[6];
      System.arraycopy(this.neuronId, 0, a, 0, 6);
      return a;
   }

   @Override
   public int hashCode() {
      return 33554432
         | ((this.neuronId[1] ^ this.neuronId[0]) & 0xFF) << 16
         | ((this.neuronId[3] ^ this.neuronId[2]) & 0xFF) << 8
         | (this.neuronId[5] ^ this.neuronId[4]) & 0xFF;
   }

   public boolean isZero() {
      for (int i = 0; i < this.neuronId.length; i++) {
         if (this.neuronId[i] != 0) {
            return false;
         }
      }

      return true;
   }

   @Override
   public boolean equals(Object obj) {
      if (!(obj instanceof BNeuronId)) {
         return false;
      } else {
         BNeuronId comp = (BNeuronId)obj;

         for (int i = 0; i < comp.neuronId.length; i++) {
            if (this.neuronId[i] != comp.neuronId[i]) {
               return false;
            }
         }

         return true;
      }
   }

   public String toString(Context context) {
      return LonByteArrayUtil.toString(this.neuronId);
   }

   public void encode(DataOutput out) throws IOException {
      out.write(this.neuronId);
   }

   public BObject decode(DataInput in) throws IOException {
      byte[] nId = new byte[6];
      in.readFully(nId, 0, 6);
      return new BNeuronId(nId);
   }

   public String encodeToString() throws IOException {
      return this.toString();
   }

   public BObject decodeFromString(String s) throws IOException {
      return new BNeuronId(LonByteArrayUtil.getBytes(s, 6));
   }

   @Override
   public int getAddressType() {
      return 2;
   }
}
