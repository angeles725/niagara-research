package javax.baja.lonworks.londata;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import javax.baja.lonworks.enums.BLonNilEnum;
import javax.baja.lonworks.io.LonInputStream;
import javax.baja.lonworks.io.LonOutputStream;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BEnum;
import javax.baja.sys.BFacets;
import javax.baja.sys.BIBoolean;
import javax.baja.sys.BIEnum;
import javax.baja.sys.BINumeric;
import javax.baja.sys.BObject;
import javax.baja.sys.Context;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public final class BLonEnum extends BLonPrimitive implements BIEnum, BINumeric, BIBoolean {
   public static final BLonEnum DEFAULT = new BLonEnum(BLonNilEnum.nil);
   public static final Type TYPE = Sys.loadType(BLonEnum.class);
   private BEnum value;

   @Override
   public Type getType() {
      return TYPE;
   }

   public static BLonEnum make(BEnum d) {
      return new BLonEnum(d);
   }

   private BLonEnum(BEnum d) {
      this.value = d;
   }

   public boolean equals(Object obj) {
      if (!(obj instanceof BLonEnum)) {
         return false;
      } else {
         BEnum objValue = ((BLonEnum)obj).value;
         return this.compare(objValue);
      }
   }

   public boolean compare(BEnum d) {
      return this.value.getClass() == d.getClass() && this.value.equals(d);
   }

   public String toString(Context context) {
      return this.value.getDisplayTag(context);
   }

   public String encodeToString() throws IOException {
      return this.encodeClass(this.value) + " " + this.value.encodeToString();
   }

   public BObject decodeFromString(String s) throws IOException {
      int typNamLen = s.indexOf(32);
      BEnum d = (BEnum)this.decodeClass(s.substring(0, typNamLen));
      return make((BEnum)d.decodeFromString(s.substring(typNamLen + 1)));
   }

   public void encode(DataOutput out) throws IOException {
      out.writeUTF(this.encodeClass(this.value));
      out.writeInt(this.value.getOrdinal());
   }

   public BObject decode(DataInput in) throws IOException {
      BEnum d = (BEnum)this.decodeClass(in.readUTF());
      return make(d.getRange().get(in.readInt()));
   }

   @Override
   public void toOutputStream(LonOutputStream out, BLonElementQualifiers e) {
      int val = this.value.getOrdinal();
      switch (e.getElemtype().getOrdinal()) {
         case 7:
            out.writeSigned8(val);
            break;
         case 8:
         default:
            throw new InvalidTypeException("Invalid datatype for LonEnum.");
         case 9:
            out.writeBit(val, e.getByteOffset(), e.getBitOffset(), e.getSize());
            break;
         case 10:
            out.writeSignedBit(val, e.getByteOffset(), e.getBitOffset(), e.getSize());
      }
   }

   @Override
   public BLonPrimitive fromInputStream(LonInputStream in, BLonElementQualifiers e) {
      int ord = 0;
      switch (e.getElemtype().getOrdinal()) {
         case 7:
            ord = in.readSigned8();
            break;
         case 8:
         default:
            throw new InvalidTypeException("Invalid datatype for LonEnum.");
         case 9:
            ord = in.readBit(e.getByteOffset(), e.getBitOffset(), e.getSize());
            break;
         case 10:
            ord = in.readSignedBit(e.getByteOffset(), e.getBitOffset(), e.getSize());
      }

      return this.value.getOrdinal() == ord ? this : make(this.value.getRange().get(ord));
   }

   @Override
   public double getDataAsDouble() {
      return this.value.getOrdinal();
   }

   @Override
   public BLonPrimitive makeFromDouble(double ordinal, BLonElementQualifiers e) {
      int ord = (int)ordinal;
      BEnum dis = this.value;
      return !dis.getRange().isOrdinal(ord) ? null : make(dis.getRange().get(ord));
   }

   @Override
   public boolean getDataAsBoolean() {
      return this.value.isActive();
   }

   @Override
   public BLonPrimitive makeFromBoolean(boolean boolValue) {
      int ord = boolValue ? 1 : 0;
      BEnum dis = this.value;
      return !dis.getRange().isOrdinal(ord) ? null : make(dis.getRange().get(ord));
   }

   @Override
   public String getDataAsString() {
      return this.value.getTag();
   }

   @Override
   public BLonPrimitive makeFromString(String tag) {
      BEnum dis = this.value;
      return !dis.getRange().isTag(tag) ? null : make(dis.getRange().get(tag));
   }

   @Override
   public BEnum getDataAsEnum(BEnum e) {
      return this.value;
   }

   @Override
   public BLonPrimitive makeFromEnum(BEnum v) {
      BLonPrimitive lp = this.makeFromOrdinal(v.getOrdinal());
      return (BLonPrimitive)(lp != null ? lp : new BLonEnum(v));
   }

   public BLonPrimitive makeFromOrdinal(int ord) {
      BEnum dis = this.value;
      return !dis.getRange().isOrdinal(ord) ? null : make(dis.getRange().get(ord));
   }

   public BEnum getEnum() {
      return this.value;
   }

   public BFacets getEnumFacets() {
      return BFacets.makeEnum(this.value.getRange());
   }

   public double getNumeric() {
      return this.getDataAsDouble();
   }

   public BFacets getNumericFacets() {
      return BFacets.NULL;
   }

   public boolean getBoolean() {
      return this.getDataAsBoolean();
   }

   public BFacets getBooleanFacets() {
      return BFacets.NULL;
   }
}
