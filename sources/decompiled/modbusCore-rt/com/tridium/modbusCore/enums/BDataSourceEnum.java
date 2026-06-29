package com.tridium.modbusCore.enums;

import javax.baja.nre.annotations.NiagaraEnum;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.annotations.Range;
import javax.baja.sys.BFrozenEnum;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraEnum(
   range = {@Range("devicePoll"), @Range("pointPoll")},
   defaultValue = "pointPoll"
)
public final class BDataSourceEnum extends BFrozenEnum {
   public static final int DEVICE_POLL = 0;
   public static final int POINT_POLL = 1;
   public static final BDataSourceEnum devicePoll = new BDataSourceEnum(0);
   public static final BDataSourceEnum pointPoll = new BDataSourceEnum(1);
   public static final BDataSourceEnum DEFAULT = pointPoll;
   public static final Type TYPE = Sys.loadType(BDataSourceEnum.class);

   public static BDataSourceEnum make(int ordinal) {
      return (BDataSourceEnum)devicePoll.getRange().get(ordinal, false);
   }

   public static BDataSourceEnum make(String tag) {
      return (BDataSourceEnum)devicePoll.getRange().get(tag);
   }

   private BDataSourceEnum(int ordinal) {
      super(ordinal);
   }

   public Type getType() {
      return TYPE;
   }
}
