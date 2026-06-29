package com.tridium.modbusCore.enums;

import javax.baja.nre.annotations.NiagaraEnum;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.annotations.Range;
import javax.baja.sys.BFrozenEnum;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraEnum(
   range = {@Range("order1032"), @Range("order3210"), @Range("order0123")}
)
public final class BDataByteOrderEnum extends BFrozenEnum {
   public static final int ORDER_1032 = 0;
   public static final int ORDER_3210 = 1;
   public static final int ORDER_0123 = 2;
   public static final BDataByteOrderEnum order1032 = new BDataByteOrderEnum(0);
   public static final BDataByteOrderEnum order3210 = new BDataByteOrderEnum(1);
   public static final BDataByteOrderEnum order0123 = new BDataByteOrderEnum(2);
   public static final BDataByteOrderEnum DEFAULT = order1032;
   public static final Type TYPE = Sys.loadType(BDataByteOrderEnum.class);

   public static BDataByteOrderEnum make(int ordinal) {
      return (BDataByteOrderEnum)order1032.getRange().get(ordinal, false);
   }

   public static BDataByteOrderEnum make(String tag) {
      return (BDataByteOrderEnum)order1032.getRange().get(tag);
   }

   private BDataByteOrderEnum(int ordinal) {
      super(ordinal);
   }

   public Type getType() {
      return TYPE;
   }
}
