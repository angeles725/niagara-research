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
      value = "bad",
      ordinal = 0
   ), @Range(
      value = "uncertain",
      ordinal = 1
   ), @Range(
      value = "unused2",
      ordinal = 2
   ), @Range(
      value = "good",
      ordinal = 3
   )}
)
public final class BOpcQuality extends BFrozenEnum {
   public static final int BAD = 0;
   public static final int UNCERTAIN = 1;
   public static final int UNUSED_2 = 2;
   public static final int GOOD = 3;
   public static final BOpcQuality bad = new BOpcQuality(0);
   public static final BOpcQuality uncertain = new BOpcQuality(1);
   public static final BOpcQuality unused2 = new BOpcQuality(2);
   public static final BOpcQuality good = new BOpcQuality(3);
   public static final BOpcQuality DEFAULT = bad;
   public static final Type TYPE = Sys.loadType(BOpcQuality.class);

   public static BOpcQuality make(int ordinal) {
      return (BOpcQuality)bad.getRange().get(ordinal, false);
   }

   public static BOpcQuality make(String tag) {
      return (BOpcQuality)bad.getRange().get(tag);
   }

   private BOpcQuality(int ordinal) {
      super(ordinal);
   }

   public Type getType() {
      return TYPE;
   }

   public static boolean isBad(int quality) {
      return (quality & 192) >> 6 == 0;
   }

   public static boolean isGood(int quality) {
      return (quality & 192) >> 6 == 3;
   }

   public static boolean isUncertain(int quality) {
      return (quality & 192) >> 6 == 1;
   }

   public static BOpcQuality getQuality(int quality) {
      int qual = quality & 192;
      qual >>= 6;
      return make(qual);
   }
}
