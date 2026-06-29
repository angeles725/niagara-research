package com.tridium.bacnet.stack.server.object;

import javax.baja.bacnet.export.BBacnetTrendLogDescriptor;
import javax.baja.bacnet.export.BIBacnetExportObject;
import javax.baja.naming.BOrd;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public class BTrendLogDeletor extends BBacnetObjectDeletor {
   public static final Type TYPE = Sys.loadType(BTrendLogDeletor.class);

   @Override
   public Type getType() {
      return TYPE;
   }

   @Override
   public boolean isObjectTypeSupported(int objectType) {
      return objectType == 20;
   }

   @Override
   public BOrd getRemoteExtensionToDelete(BIBacnetExportObject desc) {
      return ((BBacnetTrendLogDescriptor)desc).getLogOrd();
   }
}
