package com.tridium.modbusCore.enums;

import javax.baja.nre.annotations.NiagaraEnum;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.annotations.Range;
import javax.baja.sys.BFrozenEnum;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraEnum(
   range = {@Range("holdingRegister"), @Range("inputRegister"), @Range("discreteCoil"), @Range("discreteInput")}
)
public final class BRegisterTypesEnum extends BFrozenEnum {
   public static final int HOLDING_REGISTER = 0;
   public static final int INPUT_REGISTER = 1;
   public static final int DISCRETE_COIL = 2;
   public static final int DISCRETE_INPUT = 3;
   public static final BRegisterTypesEnum holdingRegister = new BRegisterTypesEnum(0);
   public static final BRegisterTypesEnum inputRegister = new BRegisterTypesEnum(1);
   public static final BRegisterTypesEnum discreteCoil = new BRegisterTypesEnum(2);
   public static final BRegisterTypesEnum discreteInput = new BRegisterTypesEnum(3);
   public static final BRegisterTypesEnum DEFAULT = holdingRegister;
   public static final Type TYPE = Sys.loadType(BRegisterTypesEnum.class);

   public static BRegisterTypesEnum make(int ordinal) {
      return (BRegisterTypesEnum)holdingRegister.getRange().get(ordinal, false);
   }

   public static BRegisterTypesEnum make(String tag) {
      return (BRegisterTypesEnum)holdingRegister.getRange().get(tag);
   }

   private BRegisterTypesEnum(int ordinal) {
      super(ordinal);
   }

   public Type getType() {
      return TYPE;
   }
}
