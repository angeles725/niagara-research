package com.tridium.modbusCore.enums;

import javax.baja.nre.annotations.NiagaraEnum;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.annotations.Range;
import javax.baja.sys.BFrozenEnum;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraEnum(
   range = {@Range("order76543210"), @Range("order67452301"), @Range("order54761032"), @Range("order45670123"), @Range("order01234567"), @Range("order10325476"), @Range("order23016745"), @Range("order32107654")}
)
public final class BDataByteOrder64BitEnum extends BFrozenEnum {
   public static final int ORDER_76543210 = 0;
   public static final int ORDER_67452301 = 1;
   public static final int ORDER_54761032 = 2;
   public static final int ORDER_45670123 = 3;
   public static final int ORDER_01234567 = 4;
   public static final int ORDER_10325476 = 5;
   public static final int ORDER_23016745 = 6;
   public static final int ORDER_32107654 = 7;
   public static final BDataByteOrder64BitEnum order76543210 = new BDataByteOrder64BitEnum(0);
   public static final BDataByteOrder64BitEnum order67452301 = new BDataByteOrder64BitEnum(1);
   public static final BDataByteOrder64BitEnum order54761032 = new BDataByteOrder64BitEnum(2);
   public static final BDataByteOrder64BitEnum order45670123 = new BDataByteOrder64BitEnum(3);
   public static final BDataByteOrder64BitEnum order01234567 = new BDataByteOrder64BitEnum(4);
   public static final BDataByteOrder64BitEnum order10325476 = new BDataByteOrder64BitEnum(5);
   public static final BDataByteOrder64BitEnum order23016745 = new BDataByteOrder64BitEnum(6);
   public static final BDataByteOrder64BitEnum order32107654 = new BDataByteOrder64BitEnum(7);
   public static final BDataByteOrder64BitEnum DEFAULT = order76543210;
   public static final Type TYPE = Sys.loadType(BDataByteOrder64BitEnum.class);

   public static BDataByteOrder64BitEnum make(int ordinal) {
      return (BDataByteOrder64BitEnum)order76543210.getRange().get(ordinal, false);
   }

   public static BDataByteOrder64BitEnum make(String tag) {
      return (BDataByteOrder64BitEnum)order76543210.getRange().get(tag);
   }

   private BDataByteOrder64BitEnum(int ordinal) {
      super(ordinal);
   }

   public Type getType() {
      return TYPE;
   }
}
