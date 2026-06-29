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
      value = "nonSpecific",
      ordinal = 0
   ), @Range(
      value = "unused1",
      ordinal = 1
   ), @Range(
      value = "unused2",
      ordinal = 2
   ), @Range(
      value = "unused3",
      ordinal = 3
   ), @Range(
      value = "unused4",
      ordinal = 4
   ), @Range(
      value = "unused5",
      ordinal = 5
   ), @Range(
      value = "localOverride",
      ordinal = 6
   ), @Range(
      value = "reserved7",
      ordinal = 7
   ), @Range(
      value = "reserved8",
      ordinal = 8
   ), @Range(
      value = "reserved9",
      ordinal = 9
   ), @Range(
      value = "reserved10",
      ordinal = 10
   ), @Range(
      value = "reserved11",
      ordinal = 11
   ), @Range(
      value = "reserved12",
      ordinal = 12
   ), @Range(
      value = "reserved13",
      ordinal = 13
   ), @Range(
      value = "reserved14",
      ordinal = 14
   ), @Range(
      value = "reserved15",
      ordinal = 15
   )}
)
public final class BOpcQualityGood extends BFrozenEnum {
   public static final int NON_SPECIFIC = 0;
   public static final int UNUSED_1 = 1;
   public static final int UNUSED_2 = 2;
   public static final int UNUSED_3 = 3;
   public static final int UNUSED_4 = 4;
   public static final int UNUSED_5 = 5;
   public static final int LOCAL_OVERRIDE = 6;
   public static final int RESERVED_7 = 7;
   public static final int RESERVED_8 = 8;
   public static final int RESERVED_9 = 9;
   public static final int RESERVED_10 = 10;
   public static final int RESERVED_11 = 11;
   public static final int RESERVED_12 = 12;
   public static final int RESERVED_13 = 13;
   public static final int RESERVED_14 = 14;
   public static final int RESERVED_15 = 15;
   public static final BOpcQualityGood nonSpecific = new BOpcQualityGood(0);
   public static final BOpcQualityGood unused1 = new BOpcQualityGood(1);
   public static final BOpcQualityGood unused2 = new BOpcQualityGood(2);
   public static final BOpcQualityGood unused3 = new BOpcQualityGood(3);
   public static final BOpcQualityGood unused4 = new BOpcQualityGood(4);
   public static final BOpcQualityGood unused5 = new BOpcQualityGood(5);
   public static final BOpcQualityGood localOverride = new BOpcQualityGood(6);
   public static final BOpcQualityGood reserved7 = new BOpcQualityGood(7);
   public static final BOpcQualityGood reserved8 = new BOpcQualityGood(8);
   public static final BOpcQualityGood reserved9 = new BOpcQualityGood(9);
   public static final BOpcQualityGood reserved10 = new BOpcQualityGood(10);
   public static final BOpcQualityGood reserved11 = new BOpcQualityGood(11);
   public static final BOpcQualityGood reserved12 = new BOpcQualityGood(12);
   public static final BOpcQualityGood reserved13 = new BOpcQualityGood(13);
   public static final BOpcQualityGood reserved14 = new BOpcQualityGood(14);
   public static final BOpcQualityGood reserved15 = new BOpcQualityGood(15);
   public static final BOpcQualityGood DEFAULT = nonSpecific;
   public static final Type TYPE = Sys.loadType(BOpcQualityGood.class);

   public static BOpcQualityGood make(int ordinal) {
      return (BOpcQualityGood)nonSpecific.getRange().get(ordinal, false);
   }

   public static BOpcQualityGood make(String tag) {
      return (BOpcQualityGood)nonSpecific.getRange().get(tag);
   }

   private BOpcQualityGood(int ordinal) {
      super(ordinal);
   }

   public Type getType() {
      return TYPE;
   }

   public static BOpcQualityGood getQuality(int quality) {
      int qual = quality & 60;
      qual >>= 2;
      return make(qual);
   }
}
