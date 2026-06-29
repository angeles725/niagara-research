package com.tridium.modbusCore.client.enums;

import javax.baja.nre.annotations.NiagaraEnum;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.annotations.Range;
import javax.baja.sys.BFrozenEnum;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraEnum(
   range = {@Range("ok"), @Range("illegalFunction"), @Range("illegalDataAddress"), @Range("illegalDataValue"), @Range("slaveDeviceFailure"), @Range("acknowledge"), @Range("slaveDeviceBusy"), @Range("negativeAcknowledge"), @Range("memoryParityError"), @Range("deviceTimeout"), @Range("gatewayPathUnavailable"), @Range("gatewayTargetDeviceFailedToRespond"), @Range("crcError"), @Range("lrcError"), @Range("otherError"), @Range("okNotActive"), @Range("down"), @Range("fault"), @Range("disabled"), @Range("unknown")}
)
public final class BCommStatusEnum extends BFrozenEnum {
   public static final int OK = 0;
   public static final int ILLEGAL_FUNCTION = 1;
   public static final int ILLEGAL_DATA_ADDRESS = 2;
   public static final int ILLEGAL_DATA_VALUE = 3;
   public static final int SLAVE_DEVICE_FAILURE = 4;
   public static final int ACKNOWLEDGE = 5;
   public static final int SLAVE_DEVICE_BUSY = 6;
   public static final int NEGATIVE_ACKNOWLEDGE = 7;
   public static final int MEMORY_PARITY_ERROR = 8;
   public static final int DEVICE_TIMEOUT = 9;
   public static final int GATEWAY_PATH_UNAVAILABLE = 10;
   public static final int GATEWAY_TARGET_DEVICE_FAILED_TO_RESPOND = 11;
   public static final int CRC_ERROR = 12;
   public static final int LRC_ERROR = 13;
   public static final int OTHER_ERROR = 14;
   public static final int OK_NOT_ACTIVE = 15;
   public static final int DOWN = 16;
   public static final int FAULT = 17;
   public static final int DISABLED = 18;
   public static final int UNKNOWN = 19;
   public static final BCommStatusEnum ok = new BCommStatusEnum(0);
   public static final BCommStatusEnum illegalFunction = new BCommStatusEnum(1);
   public static final BCommStatusEnum illegalDataAddress = new BCommStatusEnum(2);
   public static final BCommStatusEnum illegalDataValue = new BCommStatusEnum(3);
   public static final BCommStatusEnum slaveDeviceFailure = new BCommStatusEnum(4);
   public static final BCommStatusEnum acknowledge = new BCommStatusEnum(5);
   public static final BCommStatusEnum slaveDeviceBusy = new BCommStatusEnum(6);
   public static final BCommStatusEnum negativeAcknowledge = new BCommStatusEnum(7);
   public static final BCommStatusEnum memoryParityError = new BCommStatusEnum(8);
   public static final BCommStatusEnum deviceTimeout = new BCommStatusEnum(9);
   public static final BCommStatusEnum gatewayPathUnavailable = new BCommStatusEnum(10);
   public static final BCommStatusEnum gatewayTargetDeviceFailedToRespond = new BCommStatusEnum(11);
   public static final BCommStatusEnum crcError = new BCommStatusEnum(12);
   public static final BCommStatusEnum lrcError = new BCommStatusEnum(13);
   public static final BCommStatusEnum otherError = new BCommStatusEnum(14);
   public static final BCommStatusEnum okNotActive = new BCommStatusEnum(15);
   public static final BCommStatusEnum down = new BCommStatusEnum(16);
   public static final BCommStatusEnum fault = new BCommStatusEnum(17);
   public static final BCommStatusEnum disabled = new BCommStatusEnum(18);
   public static final BCommStatusEnum unknown = new BCommStatusEnum(19);
   public static final BCommStatusEnum DEFAULT = ok;
   public static final Type TYPE = Sys.loadType(BCommStatusEnum.class);

   public static BCommStatusEnum make(int ordinal) {
      return (BCommStatusEnum)ok.getRange().get(ordinal, false);
   }

   public static BCommStatusEnum make(String tag) {
      return (BCommStatusEnum)ok.getRange().get(tag);
   }

   private BCommStatusEnum(int ordinal) {
      super(ordinal);
   }

   public Type getType() {
      return TYPE;
   }
}
