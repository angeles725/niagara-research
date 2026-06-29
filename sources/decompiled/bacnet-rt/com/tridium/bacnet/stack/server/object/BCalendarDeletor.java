package com.tridium.bacnet.stack.server.object;

import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public class BCalendarDeletor extends BBacnetObjectDeletor {
   public static final Type TYPE = Sys.loadType(BCalendarDeletor.class);

   @Override
   public Type getType() {
      return TYPE;
   }

   @Override
   public boolean isObjectTypeSupported(int objectType) {
      return objectType == 6;
   }
}
