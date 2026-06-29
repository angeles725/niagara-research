package com.tridium.modbusCore.enums;

import javax.baja.nre.annotations.NiagaraEnum;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.annotations.Range;
import javax.baja.sys.BFrozenEnum;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraEnum(
   range = {@Range("hex"), @Range("decimal"), @Range("modbus")}
)
public final class BAddressFormatEnum extends BFrozenEnum {
   public static final int HEX = 0;
   public static final int DECIMAL = 1;
   public static final int MODBUS = 2;
   public static final BAddressFormatEnum hex = new BAddressFormatEnum(0);
   public static final BAddressFormatEnum decimal = new BAddressFormatEnum(1);
   public static final BAddressFormatEnum modbus = new BAddressFormatEnum(2);
   public static final BAddressFormatEnum DEFAULT = hex;
   public static final Type TYPE = Sys.loadType(BAddressFormatEnum.class);

   public static BAddressFormatEnum make(int ordinal) {
      return (BAddressFormatEnum)hex.getRange().get(ordinal, false);
   }

   public static BAddressFormatEnum make(String tag) {
      return (BAddressFormatEnum)hex.getRange().get(tag);
   }

   private BAddressFormatEnum(int ordinal) {
      super(ordinal);
   }

   public Type getType() {
      return TYPE;
   }
}
