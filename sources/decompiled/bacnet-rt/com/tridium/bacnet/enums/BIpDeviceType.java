package com.tridium.bacnet.enums;

import javax.baja.nre.annotations.NiagaraEnum;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.annotations.Range;
import javax.baja.sys.BFrozenEnum;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraEnum(
   range = {@Range("standard"), @Range("foreignDevice"), @Range("bbmd")}
)
public final class BIpDeviceType extends BFrozenEnum {
   public static final int STANDARD = 0;
   public static final int FOREIGN_DEVICE = 1;
   public static final int BBMD = 2;
   public static final BIpDeviceType standard = new BIpDeviceType(0);
   public static final BIpDeviceType foreignDevice = new BIpDeviceType(1);
   public static final BIpDeviceType bbmd = new BIpDeviceType(2);
   public static final BIpDeviceType DEFAULT = standard;
   public static final Type TYPE = Sys.loadType(BIpDeviceType.class);

   public static BIpDeviceType make(int ordinal) {
      return (BIpDeviceType)standard.getRange().get(ordinal, false);
   }

   public static BIpDeviceType make(String tag) {
      return (BIpDeviceType)standard.getRange().get(tag);
   }

   private BIpDeviceType(int ordinal) {
      super(ordinal);
   }

   public Type getType() {
      return TYPE;
   }
}
