package com.tridium.lonworks.enums;

import javax.baja.nre.annotations.NiagaraEnum;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.annotations.Range;
import javax.baja.sys.BFrozenEnum;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraEnum(
   range = {@Range("normal"), @Range("initRouterTable"), @Range("temporaryBridge"), @Range("unknown")}
)
public final class BLonRouterMode extends BFrozenEnum {
   public static final int NORMAL = 0;
   public static final int INIT_ROUTER_TABLE = 1;
   public static final int TEMPORARY_BRIDGE = 2;
   public static final int UNKNOWN = 3;
   public static final BLonRouterMode normal = new BLonRouterMode(0);
   public static final BLonRouterMode initRouterTable = new BLonRouterMode(1);
   public static final BLonRouterMode temporaryBridge = new BLonRouterMode(2);
   public static final BLonRouterMode unknown = new BLonRouterMode(3);
   public static final BLonRouterMode DEFAULT = normal;
   public static final Type TYPE = Sys.loadType(BLonRouterMode.class);

   public static BLonRouterMode make(int ordinal) {
      return (BLonRouterMode)normal.getRange().get(ordinal, false);
   }

   public static BLonRouterMode make(String tag) {
      return (BLonRouterMode)normal.getRange().get(tag);
   }

   private BLonRouterMode(int ordinal) {
      super(ordinal);
   }

   public Type getType() {
      return TYPE;
   }
}
