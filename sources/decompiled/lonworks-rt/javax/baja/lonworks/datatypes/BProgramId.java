package javax.baja.lonworks.datatypes;

import com.tridium.lonworks.util.LonByteArrayUtil;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import javax.baja.lonworks.enums.BLonMfgId;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BObject;
import javax.baja.sys.BSimple;
import javax.baja.sys.Context;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public final class BProgramId extends BSimple {
   private static final byte[] defaultId = new byte[]{0, 0, 0, 0, 0, 0, 0, 0};
   public static final BProgramId DEFAULT = new BProgramId(defaultId);
   public static final Type TYPE = Sys.loadType(BProgramId.class);
   public static final BProgramId TRIDIUM_PID = make(9, BLonMfgId.tridium, 259, 32768, 3);
   public static final int PROGRAM_ID_LENGTH = 8;
   private byte[] programId;

   public Type getType() {
      return TYPE;
   }

   public static BProgramId make(byte[] b) {
      return new BProgramId(b);
   }

   public static BProgramId make(int format, BLonMfgId mfrId, int deviceClass, int deviceSubclass, int modelNumber) {
      byte[] pId = new byte[8];
      int mfgId = mfrId.getOrdinal();
      pId[0] = (byte)((format << 4 & 240) + (mfgId >> 16 & 15));
      pId[1] = (byte)(mfgId >> 8 & 0xFF);
      pId[2] = (byte)(mfgId & 0xFF);
      pId[3] = (byte)(deviceClass >> 8 & 0xFF);
      pId[4] = (byte)(deviceClass & 0xFF);
      pId[5] = (byte)(deviceSubclass >> 8 & 0xFF);
      pId[6] = (byte)(deviceSubclass & 0xFF);
      pId[7] = (byte)(modelNumber & 0xFF);
      return make(pId);
   }

   private BProgramId(byte[] b) {
      if (b.length != 8) {
         throw new IllegalArgumentException("Invalid array length in BProgramId constructor. " + LonByteArrayUtil.toString(b));
      } else {
         this.programId = b;
      }
   }

   public byte[] getByteArray() {
      byte[] a = new byte[8];
      System.arraycopy(this.programId, 0, a, 0, 8);
      return a;
   }

   public boolean equals(Object obj) {
      if (!(obj instanceof BProgramId)) {
         return false;
      } else {
         BProgramId comp = (BProgramId)obj;

         for (int i = 0; i < comp.programId.length; i++) {
            if (this.programId[i] != comp.programId[i]) {
               return false;
            }
         }

         return true;
      }
   }

   public boolean isZero() {
      for (int i = 0; i < this.programId.length; i++) {
         if (this.programId[i] != 0) {
            return false;
         }
      }

      return true;
   }

   public String toString(Context context) {
      return LonByteArrayUtil.toString(this.programId);
   }

   public void encode(DataOutput out) throws IOException {
      out.write(this.programId);
   }

   public BObject decode(DataInput in) throws IOException {
      byte[] pId = new byte[8];
      in.readFully(pId, 0, 8);
      return new BProgramId(pId);
   }

   public String encodeToString() throws IOException {
      return this.toString(null);
   }

   public BObject decodeFromString(String s) throws IOException {
      return new BProgramId(LonByteArrayUtil.getBytes(s, 8));
   }

   public BLonMfgId getMfgId() {
      int mfgId = this.programId[0] & 15;
      mfgId = (mfgId << 8) + (this.programId[1] & 255);
      mfgId = (mfgId << 8) + (this.programId[2] & 255);

      try {
         return BLonMfgId.make(mfgId);
      } catch (Throwable var3) {
         return BLonMfgId.unknown;
      }
   }

   private int getDeviceClass() {
      return (this.programId[3] & 0xFF) << 8 | this.programId[4] & 0xFF;
   }

   public int getFormat() {
      return (this.programId[0] & 240) >> 4;
   }

   public int getDeviceSubclass() {
      return this.programId[5] & 0xFF;
   }

   public int getDeviceChannelType() {
      return this.programId[6] & 0xFF;
   }

   public int getModelNumber() {
      return this.programId[7] & 0xFF;
   }

   public boolean isRouter() {
      return this.getDeviceClass() == 257;
   }

   public int hashCode() {
      return (this.programId[0] & 128) << 23
         | ((this.programId[1] ^ this.programId[2]) & 127) << 23
         | (this.programId[3] ^ this.programId[4]) << 16
         | (this.programId[5] ^ this.programId[6]) << 8
         | this.programId[7];
   }

   public boolean hasChangeableNvs() {
      return this.isLonMarkCompliant() && (this.getDeviceSubclass() & 128) != 0;
   }

   public boolean isLonMarkCompliant() {
      int format = this.getFormat();
      return format == 8 || format == 9;
   }
}
