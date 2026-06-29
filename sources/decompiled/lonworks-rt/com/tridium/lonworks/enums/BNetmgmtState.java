package com.tridium.lonworks.enums;

import javax.baja.nre.annotations.NiagaraEnum;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.annotations.Range;
import javax.baja.sys.BFrozenEnum;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraEnum(
   range = {@Range("waitOnServicePin"), @Range("servicePinReceived"), @Range("servicePinTimeOut"), @Range("nil")}
)
public final class BNetmgmtState extends BFrozenEnum {
   public static final int WAIT_ON_SERVICE_PIN = 0;
   public static final int SERVICE_PIN_RECEIVED = 1;
   public static final int SERVICE_PIN_TIME_OUT = 2;
   public static final int NIL = 3;
   public static final BNetmgmtState waitOnServicePin = new BNetmgmtState(0);
   public static final BNetmgmtState servicePinReceived = new BNetmgmtState(1);
   public static final BNetmgmtState servicePinTimeOut = new BNetmgmtState(2);
   public static final BNetmgmtState nil = new BNetmgmtState(3);
   public static final BNetmgmtState DEFAULT = waitOnServicePin;
   public static final Type TYPE = Sys.loadType(BNetmgmtState.class);

   public static BNetmgmtState make(int ordinal) {
      return (BNetmgmtState)waitOnServicePin.getRange().get(ordinal, false);
   }

   public static BNetmgmtState make(String tag) {
      return (BNetmgmtState)waitOnServicePin.getRange().get(tag);
   }

   private BNetmgmtState(int ordinal) {
      super(ordinal);
   }

   public Type getType() {
      return TYPE;
   }
}
