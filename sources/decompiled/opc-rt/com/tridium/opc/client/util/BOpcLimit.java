package com.tridium.opc.client.util;

import javax.baja.nre.annotations.NiagaraEnum;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.annotations.Range;
import javax.baja.sys.BFrozenEnum;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraEnum(
   range = {@Range(
      value = "notLimited",
      ordinal = 0
   ), @Range(
      value = "lowLimited",
      ordinal = 1
   ), @Range(
      value = "highLimited",
      ordinal = 2
   ), @Range(
      value = "constant",
      ordinal = 3
   )}
)
public final class BOpcLimit extends BFrozenEnum {
   public static final int NOT_LIMITED = 0;
   public static final int LOW_LIMITED = 1;
   public static final int HIGH_LIMITED = 2;
   public static final int CONSTANT = 3;
   public static final BOpcLimit notLimited = new BOpcLimit(0);
   public static final BOpcLimit lowLimited = new BOpcLimit(1);
   public static final BOpcLimit highLimited = new BOpcLimit(2);
   public static final BOpcLimit constant = new BOpcLimit(3);
   public static final BOpcLimit DEFAULT = notLimited;
   public static final Type TYPE = Sys.loadType(BOpcLimit.class);

   public static BOpcLimit make(int ordinal) {
      return (BOpcLimit)notLimited.getRange().get(ordinal, false);
   }

   public static BOpcLimit make(String tag) {
      return (BOpcLimit)notLimited.getRange().get(tag);
   }

   private BOpcLimit(int ordinal) {
      super(ordinal);
   }

   public Type getType() {
      return TYPE;
   }

   public static BOpcLimit getLimit(int quality) {
      int l = quality & 3;
      return make(l);
   }
}
