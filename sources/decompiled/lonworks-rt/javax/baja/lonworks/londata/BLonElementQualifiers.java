package javax.baja.lonworks.londata;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import javax.baja.lonworks.LonException;
import javax.baja.lonworks.enums.BLonElementType;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BFloat;
import javax.baja.sys.BObject;
import javax.baja.sys.BSimple;
import javax.baja.sys.Context;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public final class BLonElementQualifiers extends BSimple {
   public static final BLonElementQualifiers DEFAULT = new BLonElementQualifiers();
   public static final Type TYPE = Sys.loadType(BLonElementQualifiers.class);
   private BLonElementType elemtype = BLonElementType.na;
   private float resolution = 1.0F;
   private float offset = 0.0F;
   private boolean hasMinimum = false;
   private Number minimum = 0.0F;
   private boolean hasMaximum = false;
   private Number maximum = 0.0F;
   private boolean hasOffset = false;
   private int byteOffset = 0;
   private int bitOffset = 0;
   private boolean hasInvalid = false;
   private Number invalidValue = 0.0F;
   private static final float DEFAULT_OFFSET = 0.0F;
   private static final int DEFAULT_B_OFFSET = 0;
   private static final int DEFAULT_LENGTH = 0;
   private int length = 0;
   public static final BLonElementQualifiers NONE = new BLonElementQualifiers();
   public static final BLonElementQualifiers ENUM = new BLonElementQualifiers(BLonElementType.e8);
   public static final BLonElementQualifiers BOOLEAN = make(BLonElementType.b8, false, 0.0F, false, 0.0F, 1.0F, 0.0F, false, 0, 0, false, 0.0F, 1);
   public static final BLonElementQualifiers SIGNED_LONG1 = new BLonElementQualifiers(BLonElementType.s16, -32768.0F, 32767.0F, 1.0F);
   public static final BLonElementQualifiers UNSIGNED_LONG1 = new BLonElementQualifiers(BLonElementType.u16, 0.0F, 65535.0F, 1.0F);
   public static final BLonElementQualifiers S32 = new BLonElementQualifiers(BLonElementType.s32);
   public static final BLonElementQualifiers U8 = new BLonElementQualifiers(BLonElementType.u8, 0.0F, 255.0F, 1.0F);
   public static final BLonElementQualifiers U16 = new BLonElementQualifiers(BLonElementType.u16, 0.0F, 65535.0F, 1.0F);
   public static final BLonElementQualifiers HOUR = new BLonElementQualifiers(BLonElementType.u8, 0.0F, 23.0F, 1.0F);
   public static final BLonElementQualifiers MINUTE = new BLonElementQualifiers(BLonElementType.u8, 0.0F, 59.0F, 1.0F);

   public Type getType() {
      return TYPE;
   }

   public static BLonElementQualifiers make(BLonElementType elemtype, int length) {
      return new BLonElementQualifiers(elemtype, length);
   }

   public static BLonElementQualifiers make(
      BLonElementType elemtype,
      boolean hasMinimum,
      float minimum,
      boolean hasMaximum,
      float maximum,
      float resolution,
      float offset,
      boolean hasOffset,
      int byteOffset,
      int bitOffset,
      boolean hasInvalid,
      float invalidValue,
      int length
   ) {
      return new BLonElementQualifiers(
         elemtype, hasMinimum, minimum, hasMaximum, maximum, resolution, offset, hasOffset, byteOffset, bitOffset, hasInvalid, invalidValue, length
      );
   }

   public static BLonElementQualifiers make(
      BLonElementType elemtype,
      boolean hasMinimum,
      Number minimum,
      boolean hasMaximum,
      Number maximum,
      float resolution,
      float offset,
      boolean hasOffset,
      int byteOffset,
      int bitOffset,
      boolean hasInvalid,
      Number invalidValue,
      int length
   ) {
      return new BLonElementQualifiers(
         elemtype, hasMinimum, minimum, hasMaximum, maximum, resolution, offset, hasOffset, byteOffset, bitOffset, hasInvalid, invalidValue, length
      );
   }

   public static BLonElementQualifiers make(BLonElementType elemtype, float minimum, float maximum, float resolution, float invalidValue) {
      return new BLonElementQualifiers(elemtype, true, minimum, true, maximum, resolution, 0.0F, false, 0, 0, true, invalidValue, 0);
   }

   private BLonElementQualifiers() {
   }

   private BLonElementQualifiers(BLonElementType elemtype) {
      this.elemtype = elemtype;
   }

   private BLonElementQualifiers(BLonElementType elemtype, int length) {
      this.elemtype = elemtype;
      this.length = length;
   }

   private BLonElementQualifiers(
      BLonElementType elemtype,
      boolean hasMinimum,
      Number minimum,
      boolean hasMaximum,
      Number maximum,
      float resolution,
      float offset,
      boolean hasOffset,
      int byteOffset,
      int bitOffset,
      boolean hasInvalid,
      Number invalidValue,
      int length
   ) {
      this.elemtype = elemtype;
      this.hasMinimum = hasMinimum;
      this.minimum = minimum;
      this.hasMaximum = hasMaximum;
      this.maximum = maximum;
      this.resolution = resolution;
      this.offset = offset;
      this.hasOffset = hasOffset;
      this.byteOffset = byteOffset;
      this.bitOffset = bitOffset;
      this.hasInvalid = hasInvalid;
      this.invalidValue = invalidValue;
      this.length = length;
   }

   private BLonElementQualifiers(BLonElementType elemtype, float minimum, float maximum, float resolution) {
      this.elemtype = elemtype;
      this.hasMinimum = true;
      this.minimum = minimum;
      this.hasMaximum = true;
      this.maximum = maximum;
      this.resolution = resolution;
   }

   private BLonElementQualifiers(BLonElementType elemtype, float minimum, float maximum, float resolution, float invalidValue) {
      this.elemtype = elemtype;
      this.hasMinimum = true;
      this.minimum = minimum;
      this.hasMaximum = true;
      this.maximum = maximum;
      this.resolution = resolution;
      this.hasInvalid = true;
      this.invalidValue = invalidValue;
   }

   public boolean equals(Object obj) {
      if (!(obj instanceof BLonElementQualifiers)) {
         return false;
      } else {
         BLonElementQualifiers comp = (BLonElementQualifiers)obj;
         return comp.elemtype.equals(this.elemtype)
            && BFloat.equals(comp.resolution, this.resolution)
            && BFloat.equals(comp.offset, this.offset)
            && comp.hasMinimum == this.hasMinimum
            && this.numberEquals(comp.minimum, this.minimum)
            && comp.hasMaximum == this.hasMaximum
            && this.numberEquals(comp.maximum, this.maximum)
            && comp.hasOffset == this.hasOffset
            && comp.byteOffset == this.byteOffset
            && comp.bitOffset == this.bitOffset
            && comp.length == this.length
            && comp.hasInvalid == this.hasInvalid
            && this.numberEquals(comp.invalidValue, this.invalidValue);
      }
   }

   private boolean numberEquals(Number comp, Number n) {
      return n instanceof Long ? comp.longValue() == n.longValue() : Double.valueOf(comp.doubleValue()).equals(n.doubleValue());
   }

   public boolean canCopyFrom(Object obj) {
      if (!(obj instanceof BLonElementQualifiers)) {
         return false;
      } else {
         BLonElementQualifiers comp = (BLonElementQualifiers)obj;
         return comp.elemtype.equals(this.elemtype)
            && BFloat.equals(comp.resolution, this.resolution)
            && BFloat.equals(comp.offset, this.offset)
            && comp.hasOffset == this.hasOffset
            && (!this.hasOffset || comp.byteOffset == this.byteOffset)
            && (!this.hasOffset || comp.bitOffset == this.bitOffset)
            && comp.length == this.length
            && comp.hasInvalid == this.hasInvalid
            && this.numberEquals(comp.invalidValue, this.invalidValue);
      }
   }

   public int getDataByteLength() throws LonException {
      switch (this.elemtype.getOrdinal()) {
         case 0:
         case 1:
         case 2:
         case 6:
         case 7:
            return 1;
         case 3:
         case 4:
            return 2;
         case 5:
         case 8:
         case 16:
            return 4;
         case 9:
         case 10:
         case 11:
         case 12:
         case 13:
            return 0;
         case 14:
         case 15:
            return this.length;
         case 17:
         case 18:
         case 19:
            return 8;
         default:
            throw new LonException("Unsupported element type in BLonElementQualifier.getDataByteLenth() " + this.elemtype);
      }
   }

   public String toString(Context context) {
      StringBuilder sb = new StringBuilder();
      sb.append(this.elemtype.getTag()).append(" ");
      sb.append("res=").append(this.resolution).append(" ");
      sb.append("off=").append(this.offset).append(" ");
      sb.append("min=").append(this.hasMinimum ? "t" : "f").append(this.minimum).append(" ");
      sb.append("max=").append(this.hasMaximum ? "t" : "f").append(this.maximum).append(" ");
      sb.append("byt=").append(this.hasOffset ? "t" : "f").append(this.byteOffset).append(" ");
      sb.append("bit=").append(this.bitOffset).append(" ");
      sb.append("len=").append(this.length).append(" ");
      sb.append("inv=").append(this.hasInvalid ? "t" : "f").append(this.invalidValue).append(" ");
      return sb.toString();
   }

   public void encode(DataOutput out) throws IOException {
      throw new IOException("BLonElementQualifiers not serialized");
   }

   public BObject decode(DataInput in) throws IOException {
      throw new IOException("BLonElementQualifiers not serialized");
   }

   public String encodeToString() throws IOException {
      throw new IOException("BLonElementQualifiers cannot encodeTostring");
   }

   public BObject decodeFromString(String s) throws IOException {
      throw new IOException("BLonElementQualifiers cannot decodeFromString");
   }

   public BLonElementType getElemtype() {
      return this.elemtype;
   }

   public float getResolution() {
      return this.resolution;
   }

   public float getOffset() {
      return this.offset;
   }

   public int getByteOffset() {
      return this.byteOffset;
   }

   public int getBitOffset() {
      return this.bitOffset;
   }

   public int getSize() {
      return this.length;
   }

   public int getLength() {
      return this.length;
   }

   public boolean hasOffset() {
      return this.hasOffset;
   }

   public boolean hasMinimum() {
      return this.hasMinimum;
   }

   public float getMinimum() {
      return this.minimum.floatValue();
   }

   public double getMinimumD() {
      return this.minimum.doubleValue();
   }

   public Number getMinimumN() {
      return this.minimum;
   }

   public boolean hasMaximum() {
      return this.hasMaximum;
   }

   public float getMaximum() {
      return this.maximum.floatValue();
   }

   public double getMaximumD() {
      return this.maximum.doubleValue();
   }

   public Number getMaximumN() {
      return this.maximum;
   }

   public boolean hasInvalidValue() {
      return this.hasInvalid;
   }

   public float getInvalidValue() {
      return this.invalidValue.floatValue();
   }

   public long getInvalidValueL() {
      return this.invalidValue.longValue();
   }

   public Number getInvalidValueN() {
      return this.invalidValue;
   }

   public boolean isInvalid(long lval) {
      return this.hasInvalidValue() && lval == this.invalidValue.longValue();
   }

   public boolean isInvalid(float fval) {
      return this.hasInvalidValue() && fval == this.invalidValue.floatValue();
   }

   public boolean isInvalid(double dval) {
      return this.hasInvalidValue() && dval == this.invalidValue.doubleValue();
   }
}
