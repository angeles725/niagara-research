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
      value = "unknown0",
      ordinal = 0
   ), @Range(
      value = "unknown1",
      ordinal = 1
   ), @Range(
      value = "unknown2",
      ordinal = 2
   ), @Range(
      value = "unknown3",
      ordinal = 3
   ), @Range(
      value = "unknown4",
      ordinal = 4
   ), @Range(
      value = "unknown5",
      ordinal = 5
   ), @Range(
      value = "unknown6",
      ordinal = 6
   ), @Range(
      value = "unknown7",
      ordinal = 7
   ), @Range(
      value = "unknown8",
      ordinal = 8
   ), @Range(
      value = "unknown9",
      ordinal = 9
   ), @Range(
      value = "unknown10",
      ordinal = 10
   ), @Range(
      value = "unknown11",
      ordinal = 11
   ), @Range(
      value = "unknown12",
      ordinal = 12
   ), @Range(
      value = "unknown13",
      ordinal = 13
   ), @Range(
      value = "unknown14",
      ordinal = 14
   ), @Range(
      value = "unknown15",
      ordinal = 15
   )}
)
public final class BOpcQualityUnknown extends BFrozenEnum {
   public static final int UNKNOWN_0 = 0;
   public static final int UNKNOWN_1 = 1;
   public static final int UNKNOWN_2 = 2;
   public static final int UNKNOWN_3 = 3;
   public static final int UNKNOWN_4 = 4;
   public static final int UNKNOWN_5 = 5;
   public static final int UNKNOWN_6 = 6;
   public static final int UNKNOWN_7 = 7;
   public static final int UNKNOWN_8 = 8;
   public static final int UNKNOWN_9 = 9;
   public static final int UNKNOWN_10 = 10;
   public static final int UNKNOWN_11 = 11;
   public static final int UNKNOWN_12 = 12;
   public static final int UNKNOWN_13 = 13;
   public static final int UNKNOWN_14 = 14;
   public static final int UNKNOWN_15 = 15;
   public static final BOpcQualityUnknown unknown0 = new BOpcQualityUnknown(0);
   public static final BOpcQualityUnknown unknown1 = new BOpcQualityUnknown(1);
   public static final BOpcQualityUnknown unknown2 = new BOpcQualityUnknown(2);
   public static final BOpcQualityUnknown unknown3 = new BOpcQualityUnknown(3);
   public static final BOpcQualityUnknown unknown4 = new BOpcQualityUnknown(4);
   public static final BOpcQualityUnknown unknown5 = new BOpcQualityUnknown(5);
   public static final BOpcQualityUnknown unknown6 = new BOpcQualityUnknown(6);
   public static final BOpcQualityUnknown unknown7 = new BOpcQualityUnknown(7);
   public static final BOpcQualityUnknown unknown8 = new BOpcQualityUnknown(8);
   public static final BOpcQualityUnknown unknown9 = new BOpcQualityUnknown(9);
   public static final BOpcQualityUnknown unknown10 = new BOpcQualityUnknown(10);
   public static final BOpcQualityUnknown unknown11 = new BOpcQualityUnknown(11);
   public static final BOpcQualityUnknown unknown12 = new BOpcQualityUnknown(12);
   public static final BOpcQualityUnknown unknown13 = new BOpcQualityUnknown(13);
   public static final BOpcQualityUnknown unknown14 = new BOpcQualityUnknown(14);
   public static final BOpcQualityUnknown unknown15 = new BOpcQualityUnknown(15);
   public static final BOpcQualityUnknown DEFAULT = unknown0;
   public static final Type TYPE = Sys.loadType(BOpcQualityUnknown.class);

   public static BOpcQualityUnknown make(int ordinal) {
      return (BOpcQualityUnknown)unknown0.getRange().get(ordinal, false);
   }

   public static BOpcQualityUnknown make(String tag) {
      return (BOpcQualityUnknown)unknown0.getRange().get(tag);
   }

   private BOpcQualityUnknown(int ordinal) {
      super(ordinal);
   }

   public Type getType() {
      return TYPE;
   }

   public static BOpcQualityUnknown getQuality(int quality) {
      int qual = quality & 60;
      qual >>= 2;
      return make(qual);
   }
}
