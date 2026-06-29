package com.tridium.modbusCore.enums;

import javax.baja.nre.annotations.NiagaraEnum;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.annotations.Range;
import javax.baja.sys.BFrozenEnum;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraEnum(
   range = {@Range("ascii"), @Range("rtu")},
   defaultValue = "rtu"
)
public final class BModbusDataModeEnum extends BFrozenEnum {
   public static final int ASCII = 0;
   public static final int RTU = 1;
   public static final BModbusDataModeEnum ascii = new BModbusDataModeEnum(0);
   public static final BModbusDataModeEnum rtu = new BModbusDataModeEnum(1);
   public static final BModbusDataModeEnum DEFAULT = rtu;
   public static final Type TYPE = Sys.loadType(BModbusDataModeEnum.class);

   public static BModbusDataModeEnum make(int ordinal) {
      return (BModbusDataModeEnum)ascii.getRange().get(ordinal, false);
   }

   public static BModbusDataModeEnum make(String tag) {
      return (BModbusDataModeEnum)ascii.getRange().get(tag);
   }

   private BModbusDataModeEnum(int ordinal) {
      super(ordinal);
   }

   public Type getType() {
      return TYPE;
   }
}
