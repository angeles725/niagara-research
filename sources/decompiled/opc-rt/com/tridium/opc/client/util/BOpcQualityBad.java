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
      value = "configError",
      ordinal = 1
   ), @Range(
      value = "notConnected",
      ordinal = 2
   ), @Range(
      value = "deviceFailure",
      ordinal = 3
   ), @Range(
      value = "sensorFailure",
      ordinal = 4
   ), @Range(
      value = "lastKnownValue",
      ordinal = 5
   ), @Range(
      value = "commFailure",
      ordinal = 6
   ), @Range(
      value = "outOfService",
      ordinal = 7
   ), @Range(
      value = "waitingForInitialData",
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
public final class BOpcQualityBad extends BFrozenEnum {
   public static final int NON_SPECIFIC = 0;
   public static final int CONFIG_ERROR = 1;
   public static final int NOT_CONNECTED = 2;
   public static final int DEVICE_FAILURE = 3;
   public static final int SENSOR_FAILURE = 4;
   public static final int LAST_KNOWN_VALUE = 5;
   public static final int COMM_FAILURE = 6;
   public static final int OUT_OF_SERVICE = 7;
   public static final int WAITING_FOR_INITIAL_DATA = 8;
   public static final int RESERVED_9 = 9;
   public static final int RESERVED_10 = 10;
   public static final int RESERVED_11 = 11;
   public static final int RESERVED_12 = 12;
   public static final int RESERVED_13 = 13;
   public static final int RESERVED_14 = 14;
   public static final int RESERVED_15 = 15;
   public static final BOpcQualityBad nonSpecific = new BOpcQualityBad(0);
   public static final BOpcQualityBad configError = new BOpcQualityBad(1);
   public static final BOpcQualityBad notConnected = new BOpcQualityBad(2);
   public static final BOpcQualityBad deviceFailure = new BOpcQualityBad(3);
   public static final BOpcQualityBad sensorFailure = new BOpcQualityBad(4);
   public static final BOpcQualityBad lastKnownValue = new BOpcQualityBad(5);
   public static final BOpcQualityBad commFailure = new BOpcQualityBad(6);
   public static final BOpcQualityBad outOfService = new BOpcQualityBad(7);
   public static final BOpcQualityBad waitingForInitialData = new BOpcQualityBad(8);
   public static final BOpcQualityBad reserved9 = new BOpcQualityBad(9);
   public static final BOpcQualityBad reserved10 = new BOpcQualityBad(10);
   public static final BOpcQualityBad reserved11 = new BOpcQualityBad(11);
   public static final BOpcQualityBad reserved12 = new BOpcQualityBad(12);
   public static final BOpcQualityBad reserved13 = new BOpcQualityBad(13);
   public static final BOpcQualityBad reserved14 = new BOpcQualityBad(14);
   public static final BOpcQualityBad reserved15 = new BOpcQualityBad(15);
   public static final BOpcQualityBad DEFAULT = nonSpecific;
   public static final Type TYPE = Sys.loadType(BOpcQualityBad.class);

   public static BOpcQualityBad make(int ordinal) {
      return (BOpcQualityBad)nonSpecific.getRange().get(ordinal, false);
   }

   public static BOpcQualityBad make(String tag) {
      return (BOpcQualityBad)nonSpecific.getRange().get(tag);
   }

   private BOpcQualityBad(int ordinal) {
      super(ordinal);
   }

   public Type getType() {
      return TYPE;
   }

   public static BOpcQualityBad getQuality(int quality) {
      int qual = quality & 60;
      qual >>= 2;
      return make(qual);
   }
}
