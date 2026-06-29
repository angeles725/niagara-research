package com.tridium.lonworks.enums;

import javax.baja.nre.annotations.NiagaraEnum;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.annotations.Range;
import javax.baja.sys.BFrozenEnum;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraEnum(
   range = {@Range("idle"), @Range("waiting"), @Range("received"), @Range("canceled")}
)
public final class BServicePinState extends BFrozenEnum {
   public static final int IDLE = 0;
   public static final int WAITING = 1;
   public static final int RECEIVED = 2;
   public static final int CANCELED = 3;
   public static final BServicePinState idle = new BServicePinState(0);
   public static final BServicePinState waiting = new BServicePinState(1);
   public static final BServicePinState received = new BServicePinState(2);
   public static final BServicePinState canceled = new BServicePinState(3);
   public static final BServicePinState DEFAULT = idle;
   public static final Type TYPE = Sys.loadType(BServicePinState.class);

   public static BServicePinState make(int ordinal) {
      return (BServicePinState)idle.getRange().get(ordinal, false);
   }

   public static BServicePinState make(String tag) {
      return (BServicePinState)idle.getRange().get(tag);
   }

   private BServicePinState(int ordinal) {
      super(ordinal);
   }

   public Type getType() {
      return TYPE;
   }
}
