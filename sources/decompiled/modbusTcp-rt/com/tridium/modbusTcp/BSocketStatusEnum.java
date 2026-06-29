package com.tridium.modbusTcp;

import javax.baja.nre.annotations.NiagaraEnum;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.annotations.Range;
import javax.baja.sys.BFrozenEnum;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraEnum(
   range = {@Range("closed"), @Range("openPending"), @Range("openFailed"), @Range("opened")}
)
public final class BSocketStatusEnum extends BFrozenEnum {
   public static final int CLOSED = 0;
   public static final int OPEN_PENDING = 1;
   public static final int OPEN_FAILED = 2;
   public static final int OPENED = 3;
   public static final BSocketStatusEnum closed = new BSocketStatusEnum(0);
   public static final BSocketStatusEnum openPending = new BSocketStatusEnum(1);
   public static final BSocketStatusEnum openFailed = new BSocketStatusEnum(2);
   public static final BSocketStatusEnum opened = new BSocketStatusEnum(3);
   public static final BSocketStatusEnum DEFAULT = closed;
   public static final Type TYPE = Sys.loadType(BSocketStatusEnum.class);

   public static BSocketStatusEnum make(int ordinal) {
      return (BSocketStatusEnum)closed.getRange().get(ordinal, false);
   }

   public static BSocketStatusEnum make(String tag) {
      return (BSocketStatusEnum)closed.getRange().get(tag);
   }

   private BSocketStatusEnum(int ordinal) {
      super(ordinal);
   }

   public Type getType() {
      return TYPE;
   }
}
