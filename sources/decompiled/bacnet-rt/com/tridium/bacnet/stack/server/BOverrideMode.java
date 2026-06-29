package com.tridium.bacnet.stack.server;

import javax.baja.nre.annotations.NiagaraEnum;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.annotations.Range;
import javax.baja.sys.BFrozenEnum;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraEnum(
   range = {@Range("legacy"), @Range("oneOnly")},
   defaultValue = "legacy"
)
public final class BOverrideMode extends BFrozenEnum {
   public static final int LEGACY = 0;
   public static final int ONE_ONLY = 1;
   public static final BOverrideMode legacy = new BOverrideMode(0);
   public static final BOverrideMode oneOnly = new BOverrideMode(1);
   public static final BOverrideMode DEFAULT = legacy;
   public static final Type TYPE = Sys.loadType(BOverrideMode.class);

   public static BOverrideMode make(int ordinal) {
      return (BOverrideMode)legacy.getRange().get(ordinal, false);
   }

   public static BOverrideMode make(String tag) {
      return (BOverrideMode)legacy.getRange().get(tag);
   }

   private BOverrideMode(int ordinal) {
      super(ordinal);
   }

   public Type getType() {
      return TYPE;
   }
}
