package com.tridium.modbusCore.enums;

import javax.baja.nre.annotations.NiagaraEnum;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.annotations.Range;
import javax.baja.sys.BFrozenEnum;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraEnum(
   range = {@Range("useNetworkDataMode"), @Range("ascii"), @Range("rtu")}
)
public final class BDeviceDataModeEnum extends BFrozenEnum {
   public static final int USE_NETWORK_DATA_MODE = 0;
   public static final int ASCII = 1;
   public static final int RTU = 2;
   public static final BDeviceDataModeEnum useNetworkDataMode = new BDeviceDataModeEnum(0);
   public static final BDeviceDataModeEnum ascii = new BDeviceDataModeEnum(1);
   public static final BDeviceDataModeEnum rtu = new BDeviceDataModeEnum(2);
   public static final BDeviceDataModeEnum DEFAULT = useNetworkDataMode;
   public static final Type TYPE = Sys.loadType(BDeviceDataModeEnum.class);

   public static BDeviceDataModeEnum make(int ordinal) {
      return (BDeviceDataModeEnum)useNetworkDataMode.getRange().get(ordinal, false);
   }

   public static BDeviceDataModeEnum make(String tag) {
      return (BDeviceDataModeEnum)useNetworkDataMode.getRange().get(tag);
   }

   private BDeviceDataModeEnum(int ordinal) {
      super(ordinal);
   }

   public Type getType() {
      return TYPE;
   }
}
