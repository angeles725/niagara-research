package com.tridium.lonworks.enums;

import javax.baja.nre.annotations.NiagaraEnum;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.annotations.Range;
import javax.baja.sys.BFrozenEnum;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraEnum(
   range = {@Range("configured"), @Range("learning"), @Range("bridge"), @Range("repeater"), @Range("unknown")}
)
public final class BLonRouterType extends BFrozenEnum {
   public static final int CONFIGURED = 0;
   public static final int LEARNING = 1;
   public static final int BRIDGE = 2;
   public static final int REPEATER = 3;
   public static final int UNKNOWN = 4;
   public static final BLonRouterType configured = new BLonRouterType(0);
   public static final BLonRouterType learning = new BLonRouterType(1);
   public static final BLonRouterType bridge = new BLonRouterType(2);
   public static final BLonRouterType repeater = new BLonRouterType(3);
   public static final BLonRouterType unknown = new BLonRouterType(4);
   public static final BLonRouterType DEFAULT = configured;
   public static final Type TYPE = Sys.loadType(BLonRouterType.class);

   public static BLonRouterType make(int ordinal) {
      return (BLonRouterType)configured.getRange().get(ordinal, false);
   }

   public static BLonRouterType make(String tag) {
      return (BLonRouterType)configured.getRange().get(tag);
   }

   private BLonRouterType(int ordinal) {
      super(ordinal);
   }

   public Type getType() {
      return TYPE;
   }
}
