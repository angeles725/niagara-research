package javax.baja.lonworks.enums;

import javax.baja.nre.annotations.NiagaraEnum;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.annotations.Range;
import javax.baja.sys.BFrozenEnum;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraEnum(
   range = {@Range(
      value = "nvtCatInitial",
      ordinal = 0
   ), @Range(
      value = "nvtCatSignedChar",
      ordinal = 1
   ), @Range(
      value = "nvtCatUnsignedChar",
      ordinal = 2
   ), @Range(
      value = "nvtCatSignedShort",
      ordinal = 3
   ), @Range(
      value = "nvtCatUnsignedShort",
      ordinal = 4
   ), @Range(
      value = "nvtCatSignedLong",
      ordinal = 5
   ), @Range(
      value = "nvtCatUnsignedLong",
      ordinal = 6
   ), @Range(
      value = "nvtCatEnum",
      ordinal = 7
   ), @Range(
      value = "nvtCatArray",
      ordinal = 8
   ), @Range(
      value = "nvtCatStruct",
      ordinal = 9
   ), @Range(
      value = "nvtCatUnion",
      ordinal = 10
   ), @Range(
      value = "nvtCatBitfield",
      ordinal = 11
   ), @Range(
      value = "nvtCatFloat",
      ordinal = 12
   ), @Range(
      value = "nvtCatSignedQuad",
      ordinal = 13
   ), @Range(
      value = "nvtCatReference",
      ordinal = 14
   ), @Range(
      value = "nvtCatNul",
      ordinal = -1
   )}
)
public final class BLonNvTypeCategoryEnum extends BFrozenEnum {
   public static final int NVT_CAT_INITIAL = 0;
   public static final int NVT_CAT_SIGNED_CHAR = 1;
   public static final int NVT_CAT_UNSIGNED_CHAR = 2;
   public static final int NVT_CAT_SIGNED_SHORT = 3;
   public static final int NVT_CAT_UNSIGNED_SHORT = 4;
   public static final int NVT_CAT_SIGNED_LONG = 5;
   public static final int NVT_CAT_UNSIGNED_LONG = 6;
   public static final int NVT_CAT_ENUM = 7;
   public static final int NVT_CAT_ARRAY = 8;
   public static final int NVT_CAT_STRUCT = 9;
   public static final int NVT_CAT_UNION = 10;
   public static final int NVT_CAT_BITFIELD = 11;
   public static final int NVT_CAT_FLOAT = 12;
   public static final int NVT_CAT_SIGNED_QUAD = 13;
   public static final int NVT_CAT_REFERENCE = 14;
   public static final int NVT_CAT_NUL = -1;
   public static final BLonNvTypeCategoryEnum nvtCatInitial = new BLonNvTypeCategoryEnum(0);
   public static final BLonNvTypeCategoryEnum nvtCatSignedChar = new BLonNvTypeCategoryEnum(1);
   public static final BLonNvTypeCategoryEnum nvtCatUnsignedChar = new BLonNvTypeCategoryEnum(2);
   public static final BLonNvTypeCategoryEnum nvtCatSignedShort = new BLonNvTypeCategoryEnum(3);
   public static final BLonNvTypeCategoryEnum nvtCatUnsignedShort = new BLonNvTypeCategoryEnum(4);
   public static final BLonNvTypeCategoryEnum nvtCatSignedLong = new BLonNvTypeCategoryEnum(5);
   public static final BLonNvTypeCategoryEnum nvtCatUnsignedLong = new BLonNvTypeCategoryEnum(6);
   public static final BLonNvTypeCategoryEnum nvtCatEnum = new BLonNvTypeCategoryEnum(7);
   public static final BLonNvTypeCategoryEnum nvtCatArray = new BLonNvTypeCategoryEnum(8);
   public static final BLonNvTypeCategoryEnum nvtCatStruct = new BLonNvTypeCategoryEnum(9);
   public static final BLonNvTypeCategoryEnum nvtCatUnion = new BLonNvTypeCategoryEnum(10);
   public static final BLonNvTypeCategoryEnum nvtCatBitfield = new BLonNvTypeCategoryEnum(11);
   public static final BLonNvTypeCategoryEnum nvtCatFloat = new BLonNvTypeCategoryEnum(12);
   public static final BLonNvTypeCategoryEnum nvtCatSignedQuad = new BLonNvTypeCategoryEnum(13);
   public static final BLonNvTypeCategoryEnum nvtCatReference = new BLonNvTypeCategoryEnum(14);
   public static final BLonNvTypeCategoryEnum nvtCatNul = new BLonNvTypeCategoryEnum(-1);
   public static final BLonNvTypeCategoryEnum DEFAULT = nvtCatInitial;
   public static final Type TYPE = Sys.loadType(BLonNvTypeCategoryEnum.class);

   public static BLonNvTypeCategoryEnum make(int ordinal) {
      return (BLonNvTypeCategoryEnum)nvtCatInitial.getRange().get(ordinal, false);
   }

   public static BLonNvTypeCategoryEnum make(String tag) {
      return (BLonNvTypeCategoryEnum)nvtCatInitial.getRange().get(tag);
   }

   private BLonNvTypeCategoryEnum(int ordinal) {
      super(ordinal);
   }

   public Type getType() {
      return TYPE;
   }
}
