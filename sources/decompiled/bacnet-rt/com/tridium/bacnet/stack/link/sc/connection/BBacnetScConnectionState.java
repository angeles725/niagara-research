package com.tridium.bacnet.stack.link.sc.connection;

import javax.baja.nre.annotations.NiagaraEnum;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.annotations.Range;
import javax.baja.sys.BFrozenEnum;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraEnum(
   range = {@Range(
      value = "notConnected",
      ordinal = 0
   ), @Range(
      value = "connected",
      ordinal = 1
   ), @Range(
      value = "disconnectedWithErrors",
      ordinal = 2
   ), @Range(
      value = "failedToConnect",
      ordinal = 3
   )},
   defaultValue = "notConnected"
)
public final class BBacnetScConnectionState extends BFrozenEnum {
   public static final int NOT_CONNECTED = 0;
   public static final int CONNECTED = 1;
   public static final int DISCONNECTED_WITH_ERRORS = 2;
   public static final int FAILED_TO_CONNECT = 3;
   public static final BBacnetScConnectionState notConnected = new BBacnetScConnectionState(0);
   public static final BBacnetScConnectionState connected = new BBacnetScConnectionState(1);
   public static final BBacnetScConnectionState disconnectedWithErrors = new BBacnetScConnectionState(2);
   public static final BBacnetScConnectionState failedToConnect = new BBacnetScConnectionState(3);
   public static final BBacnetScConnectionState DEFAULT = notConnected;
   public static final Type TYPE = Sys.loadType(BBacnetScConnectionState.class);

   public static BBacnetScConnectionState make(int ordinal) {
      return (BBacnetScConnectionState)notConnected.getRange().get(ordinal, false);
   }

   public static BBacnetScConnectionState make(String tag) {
      return (BBacnetScConnectionState)notConnected.getRange().get(tag);
   }

   private BBacnetScConnectionState(int ordinal) {
      super(ordinal);
   }

   public Type getType() {
      return TYPE;
   }
}
