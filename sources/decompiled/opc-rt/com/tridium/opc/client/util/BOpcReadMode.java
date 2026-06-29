package com.tridium.opc.client.util;

import javax.baja.nre.annotations.NiagaraEnum;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.annotations.Range;
import javax.baja.sys.BFrozenEnum;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraEnum(
   range = {@Range("cov"), @Range("sync"), @Range("async")}
)
public final class BOpcReadMode extends BFrozenEnum {
   public static final int COV = 0;
   public static final int SYNC = 1;
   public static final int ASYNC = 2;
   public static final BOpcReadMode cov = new BOpcReadMode(0);
   public static final BOpcReadMode sync = new BOpcReadMode(1);
   public static final BOpcReadMode async = new BOpcReadMode(2);
   public static final BOpcReadMode DEFAULT = cov;
   public static final Type TYPE = Sys.loadType(BOpcReadMode.class);

   public static BOpcReadMode make(int ordinal) {
      return (BOpcReadMode)cov.getRange().get(ordinal, false);
   }

   public static BOpcReadMode make(String tag) {
      return (BOpcReadMode)cov.getRange().get(tag);
   }

   private BOpcReadMode(int ordinal) {
      super(ordinal);
   }

   public Type getType() {
      return TYPE;
   }
}
