package com.tridium.opc.client.util;

import javax.baja.nre.annotations.NiagaraEnum;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.annotations.Range;
import javax.baja.sys.BFrozenEnum;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraEnum(
   range = {@Range("sync"), @Range("async")}
)
public final class BOpcWriteMode extends BFrozenEnum {
   public static final int SYNC = 0;
   public static final int ASYNC = 1;
   public static final BOpcWriteMode sync = new BOpcWriteMode(0);
   public static final BOpcWriteMode async = new BOpcWriteMode(1);
   public static final BOpcWriteMode DEFAULT = sync;
   public static final Type TYPE = Sys.loadType(BOpcWriteMode.class);

   public static BOpcWriteMode make(int ordinal) {
      return (BOpcWriteMode)sync.getRange().get(ordinal, false);
   }

   public static BOpcWriteMode make(String tag) {
      return (BOpcWriteMode)sync.getRange().get(tag);
   }

   private BOpcWriteMode(int ordinal) {
      super(ordinal);
   }

   public Type getType() {
      return TYPE;
   }
}
