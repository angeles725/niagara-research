package com.tridium.bacnet.enums;

import javax.baja.nre.annotations.NiagaraEnum;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.annotations.Range;
import javax.baja.sys.BFrozenEnum;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraEnum(
   range = {@Range(
      value = "baud_9600",
      ordinal = 9600
   ), @Range(
      value = "baud_19200",
      ordinal = 19200
   ), @Range(
      value = "baud_38400",
      ordinal = 38400
   ), @Range(
      value = "baud_57600",
      ordinal = 57600
   ), @Range(
      value = "baud_76800",
      ordinal = 76800
   ), @Range(
      value = "baud_115200",
      ordinal = 115200
   )}
)
public final class BBacnetMstpBaudRate extends BFrozenEnum {
   public static final int BAUD_9600 = 9600;
   public static final int BAUD_19200 = 19200;
   public static final int BAUD_38400 = 38400;
   public static final int BAUD_57600 = 57600;
   public static final int BAUD_76800 = 76800;
   public static final int BAUD_115200 = 115200;
   public static final BBacnetMstpBaudRate baud_9600 = new BBacnetMstpBaudRate(9600);
   public static final BBacnetMstpBaudRate baud_19200 = new BBacnetMstpBaudRate(19200);
   public static final BBacnetMstpBaudRate baud_38400 = new BBacnetMstpBaudRate(38400);
   public static final BBacnetMstpBaudRate baud_57600 = new BBacnetMstpBaudRate(57600);
   public static final BBacnetMstpBaudRate baud_76800 = new BBacnetMstpBaudRate(76800);
   public static final BBacnetMstpBaudRate baud_115200 = new BBacnetMstpBaudRate(115200);
   public static final BBacnetMstpBaudRate DEFAULT = baud_9600;
   public static final Type TYPE = Sys.loadType(BBacnetMstpBaudRate.class);

   public static BBacnetMstpBaudRate make(int ordinal) {
      return (BBacnetMstpBaudRate)baud_9600.getRange().get(ordinal, false);
   }

   public static BBacnetMstpBaudRate make(String tag) {
      return (BBacnetMstpBaudRate)baud_9600.getRange().get(tag);
   }

   private BBacnetMstpBaudRate(int ordinal) {
      super(ordinal);
   }

   public Type getType() {
      return TYPE;
   }
}
