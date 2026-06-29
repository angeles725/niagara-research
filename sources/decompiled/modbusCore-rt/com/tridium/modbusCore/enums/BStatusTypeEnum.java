package com.tridium.modbusCore.enums;

import javax.baja.nre.annotations.NiagaraEnum;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.annotations.Range;
import javax.baja.sys.BFrozenEnum;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraEnum(
   range = {@Range("coil"), @Range("input")},
   defaultValue = "input"
)
public final class BStatusTypeEnum extends BFrozenEnum {
   public static final int COIL = 0;
   public static final int INPUT = 1;
   public static final BStatusTypeEnum coil = new BStatusTypeEnum(0);
   public static final BStatusTypeEnum input = new BStatusTypeEnum(1);
   public static final BStatusTypeEnum DEFAULT = input;
   public static final Type TYPE = Sys.loadType(BStatusTypeEnum.class);

   public static BStatusTypeEnum make(int ordinal) {
      return (BStatusTypeEnum)coil.getRange().get(ordinal, false);
   }

   public static BStatusTypeEnum make(String tag) {
      return (BStatusTypeEnum)coil.getRange().get(tag);
   }

   private BStatusTypeEnum(int ordinal) {
      super(ordinal);
   }

   public Type getType() {
      return TYPE;
   }
}
