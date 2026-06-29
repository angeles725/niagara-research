package com.tridium.modbusCore.enums;

import javax.baja.nre.annotations.NiagaraEnum;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.annotations.Range;
import javax.baja.sys.BFrozenEnum;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraEnum(
   range = {@Range("integerType"), @Range("longType"), @Range("floatType"), @Range("signedInteger"), @Range("unsignedLong"), @Range("doubleType"), @Range("signed64BitLongType"), @Range("unsigned64BitLongType")}
)
public final class BDataTypeEnum extends BFrozenEnum {
   public static final int INTEGER_TYPE = 0;
   public static final int LONG_TYPE = 1;
   public static final int FLOAT_TYPE = 2;
   public static final int SIGNED_INTEGER = 3;
   public static final int UNSIGNED_LONG = 4;
   public static final int DOUBLE_TYPE = 5;
   public static final int SIGNED_64BIT_LONG_TYPE = 6;
   public static final int UNSIGNED_64BIT_LONG_TYPE = 7;
   public static final BDataTypeEnum integerType = new BDataTypeEnum(0);
   public static final BDataTypeEnum longType = new BDataTypeEnum(1);
   public static final BDataTypeEnum floatType = new BDataTypeEnum(2);
   public static final BDataTypeEnum signedInteger = new BDataTypeEnum(3);
   public static final BDataTypeEnum unsignedLong = new BDataTypeEnum(4);
   public static final BDataTypeEnum doubleType = new BDataTypeEnum(5);
   public static final BDataTypeEnum signed64BitLongType = new BDataTypeEnum(6);
   public static final BDataTypeEnum unsigned64BitLongType = new BDataTypeEnum(7);
   public static final BDataTypeEnum DEFAULT = integerType;
   public static final Type TYPE = Sys.loadType(BDataTypeEnum.class);

   public static BDataTypeEnum make(int ordinal) {
      return (BDataTypeEnum)integerType.getRange().get(ordinal, false);
   }

   public static BDataTypeEnum make(String tag) {
      return (BDataTypeEnum)integerType.getRange().get(tag);
   }

   private BDataTypeEnum(int ordinal) {
      super(ordinal);
   }

   public Type getType() {
      return TYPE;
   }
}
