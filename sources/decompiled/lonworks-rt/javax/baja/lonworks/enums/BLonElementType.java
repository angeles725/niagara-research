package javax.baja.lonworks.enums;

import javax.baja.nre.annotations.NiagaraEnum;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.annotations.Range;
import javax.baja.sys.BFrozenEnum;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraEnum(
   range = {@Range("c8"), @Range("s8"), @Range("u8"), @Range("s16"), @Range("u16"), @Range("s32"), @Range("b8"), @Range("e8"), @Range("f32"), @Range("eb"), @Range("esb"), @Range("bb"), @Range("ub"), @Range("sb"), @Range("st"), @Range("na"), @Range("u32"), @Range("f64"), @Range("s64"), @Range("u64")}
)
public final class BLonElementType extends BFrozenEnum {
   public static final int C_8 = 0;
   public static final int S_8 = 1;
   public static final int U_8 = 2;
   public static final int S_16 = 3;
   public static final int U_16 = 4;
   public static final int S_32 = 5;
   public static final int B_8 = 6;
   public static final int E_8 = 7;
   public static final int F_32 = 8;
   public static final int EB = 9;
   public static final int ESB = 10;
   public static final int BB = 11;
   public static final int UB = 12;
   public static final int SB = 13;
   public static final int ST = 14;
   public static final int NA = 15;
   public static final int U_32 = 16;
   public static final int F_64 = 17;
   public static final int S_64 = 18;
   public static final int U_64 = 19;
   public static final BLonElementType c8 = new BLonElementType(0);
   public static final BLonElementType s8 = new BLonElementType(1);
   public static final BLonElementType u8 = new BLonElementType(2);
   public static final BLonElementType s16 = new BLonElementType(3);
   public static final BLonElementType u16 = new BLonElementType(4);
   public static final BLonElementType s32 = new BLonElementType(5);
   public static final BLonElementType b8 = new BLonElementType(6);
   public static final BLonElementType e8 = new BLonElementType(7);
   public static final BLonElementType f32 = new BLonElementType(8);
   public static final BLonElementType eb = new BLonElementType(9);
   public static final BLonElementType esb = new BLonElementType(10);
   public static final BLonElementType bb = new BLonElementType(11);
   public static final BLonElementType ub = new BLonElementType(12);
   public static final BLonElementType sb = new BLonElementType(13);
   public static final BLonElementType st = new BLonElementType(14);
   public static final BLonElementType na = new BLonElementType(15);
   public static final BLonElementType u32 = new BLonElementType(16);
   public static final BLonElementType f64 = new BLonElementType(17);
   public static final BLonElementType s64 = new BLonElementType(18);
   public static final BLonElementType u64 = new BLonElementType(19);
   public static final BLonElementType DEFAULT = c8;
   public static final Type TYPE = Sys.loadType(BLonElementType.class);
   public static final int C8 = 0;
   public static final int S8 = 1;
   public static final int U8 = 2;
   public static final int S16 = 3;
   public static final int U16 = 4;
   public static final int S32 = 5;
   public static final int B8 = 6;
   public static final int E8 = 7;
   public static final int F32 = 8;
   public static final int U32 = 16;
   public static final int F64 = 17;
   public static final int S64 = 18;
   public static final int U64 = 19;

   public static BLonElementType make(int ordinal) {
      return (BLonElementType)c8.getRange().get(ordinal, false);
   }

   public static BLonElementType make(String tag) {
      return (BLonElementType)c8.getRange().get(tag);
   }

   private BLonElementType(int ordinal) {
      super(ordinal);
   }

   public Type getType() {
      return TYPE;
   }
}
