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
      value = "lastUsableValue",
      ordinal = 1
   ), @Range(
      value = "unused2",
      ordinal = 2
   ), @Range(
      value = "unused3",
      ordinal = 3
   ), @Range(
      value = "sensorNotAccurate",
      ordinal = 4
   ), @Range(
      value = "unitsExceeded",
      ordinal = 5
   ), @Range(
      value = "subNormal",
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
public final class BOpcQualityUncertain extends BFrozenEnum {
   public static final int NON_SPECIFIC = 0;
   public static final int LAST_USABLE_VALUE = 1;
   public static final int UNUSED_2 = 2;
   public static final int UNUSED_3 = 3;
   public static final int SENSOR_NOT_ACCURATE = 4;
   public static final int UNITS_EXCEEDED = 5;
   public static final int SUB_NORMAL = 6;
   public static final int RESERVED_7 = 7;
   public static final int RESERVED_8 = 8;
   public static final int RESERVED_9 = 9;
   public static final int RESERVED_10 = 10;
   public static final int RESERVED_11 = 11;
   public static final int RESERVED_12 = 12;
   public static final int RESERVED_13 = 13;
   public static final int RESERVED_14 = 14;
   public static final int RESERVED_15 = 15;
   public static final BOpcQualityUncertain nonSpecific = new BOpcQualityUncertain(0);
   public static final BOpcQualityUncertain lastUsableValue = new BOpcQualityUncertain(1);
   public static final BOpcQualityUncertain unused2 = new BOpcQualityUncertain(2);
   public static final BOpcQualityUncertain unused3 = new BOpcQualityUncertain(3);
   public static final BOpcQualityUncertain sensorNotAccurate = new BOpcQualityUncertain(4);
   public static final BOpcQualityUncertain unitsExceeded = new BOpcQualityUncertain(5);
   public static final BOpcQualityUncertain subNormal = new BOpcQualityUncertain(6);
   public static final BOpcQualityUncertain reserved7 = new BOpcQualityUncertain(7);
   public static final BOpcQualityUncertain reserved8 = new BOpcQualityUncertain(8);
   public static final BOpcQualityUncertain reserved9 = new BOpcQualityUncertain(9);
   public static final BOpcQualityUncertain reserved10 = new BOpcQualityUncertain(10);
   public static final BOpcQualityUncertain reserved11 = new BOpcQualityUncertain(11);
   public static final BOpcQualityUncertain reserved12 = new BOpcQualityUncertain(12);
   public static final BOpcQualityUncertain reserved13 = new BOpcQualityUncertain(13);
   public static final BOpcQualityUncertain reserved14 = new BOpcQualityUncertain(14);
   public static final BOpcQualityUncertain reserved15 = new BOpcQualityUncertain(15);
   public static final BOpcQualityUncertain DEFAULT = nonSpecific;
   public static final Type TYPE = Sys.loadType(BOpcQualityUncertain.class);

   public static BOpcQualityUncertain make(int ordinal) {
      return (BOpcQualityUncertain)nonSpecific.getRange().get(ordinal, false);
   }

   public static BOpcQualityUncertain make(String tag) {
      return (BOpcQualityUncertain)nonSpecific.getRange().get(tag);
   }

   private BOpcQualityUncertain(int ordinal) {
      super(ordinal);
   }

   public Type getType() {
      return TYPE;
   }

   public static BOpcQualityUncertain getQuality(int quality) {
      int qual = quality & 60;
      qual >>= 2;
      return make(qual);
   }
}
