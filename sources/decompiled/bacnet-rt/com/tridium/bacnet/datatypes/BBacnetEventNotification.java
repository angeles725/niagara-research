package com.tridium.bacnet.datatypes;

import com.tridium.bacnet.asn.EventNotificationParameters;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BStruct;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public class BBacnetEventNotification extends BStruct {
   public static final Type TYPE = Sys.loadType(BBacnetEventNotification.class);
   private final EventNotificationParameters eventNotificationParameters;

   public Type getType() {
      return TYPE;
   }

   public BBacnetEventNotification(EventNotificationParameters eventNotificationParameters) {
      this.eventNotificationParameters = eventNotificationParameters;
   }

   public EventNotificationParameters getEventNotificationParameters() {
      return this.eventNotificationParameters;
   }
}
