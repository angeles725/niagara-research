package com.tridium.opcUaServer.event;

import javax.baja.alarm.BAlarmClass;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public class BOpcUaAlarmClass extends BAlarmClass {
   public static final Type TYPE = Sys.loadType(BOpcUaAlarmClass.class);

   public Type getType() {
      return TYPE;
   }
}
