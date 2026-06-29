package com.tridium.bacnet.util;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.bacnet.BBacnetNetwork;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.export.BLocalBacnetDevice;
import javax.baja.naming.BOrd;
import javax.baja.sys.BComponent;
import javax.baja.sys.BObject;

public final class BacnetAlarmRecipientUtil {
   private static final Logger logger = Logger.getLogger("bacnet.server");

   private BacnetAlarmRecipientUtil() {
   }

   public static BBacnetObjectIdentifier getEventObjectId(BObject alarmSource) {
      if (!(alarmSource instanceof BComponent)) {
         if (logger.isLoggable(Level.FINE)) {
            if (alarmSource == null) {
               logger.fine("alarmSource is null");
            } else {
               logger.fine("alarmSource is not a component");
            }
         }

         return null;
      } else {
         BComponent eventObject = ((BComponent)alarmSource).getParent().asComponent();
         BOrd eventObjectOrd = eventObject.getHandleOrd();
         BLocalBacnetDevice localDevice = BBacnetNetwork.localDevice();
         BBacnetObjectIdentifier eventObjectId = localDevice.lookupBacnetObjectId(eventObjectOrd);
         if (eventObjectId == null) {
            if (logger.isLoggable(Level.FINE)) {
               logger.fine("alarmSource's parent " + eventObject.getSlotPath() + " is not exported to BACnet");
            }

            return null;
         } else {
            BOrd alarmOrd = ((BComponent)alarmSource).getHandleOrd();
            BBacnetObjectIdentifier eventEnrollmentId = localDevice.lookupBacnetObjectId(alarmOrd);
            return eventEnrollmentId == null ? eventObjectId : eventEnrollmentId;
         }
      }
   }
}
