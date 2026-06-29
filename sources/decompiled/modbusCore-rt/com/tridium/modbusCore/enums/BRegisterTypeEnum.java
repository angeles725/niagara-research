package com.tridium.modbusCore.enums;

import javax.baja.nre.annotations.NiagaraEnum;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.annotations.Range;
import javax.baja.sys.BFrozenEnum;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraEnum(
   range = {@Range("holding"), @Range("input")}
)
public final class BRegisterTypeEnum extends BFrozenEnum {
   public static final int HOLDING = 0;
   public static final int INPUT = 1;
   public static final BRegisterTypeEnum holding = new BRegisterTypeEnum(0);
   public static final BRegisterTypeEnum input = new BRegisterTypeEnum(1);
   public static final BRegisterTypeEnum DEFAULT = holding;
   public static final Type TYPE = Sys.loadType(BRegisterTypeEnum.class);

   public static BRegisterTypeEnum make(int ordinal) {
      return (BRegisterTypeEnum)holding.getRange().get(ordinal, false);
   }

   public static BRegisterTypeEnum make(String tag) {
      return (BRegisterTypeEnum)holding.getRange().get(tag);
   }

   private BRegisterTypeEnum(int ordinal) {
      super(ordinal);
   }

   public Type getType() {
      return TYPE;
   }
}
