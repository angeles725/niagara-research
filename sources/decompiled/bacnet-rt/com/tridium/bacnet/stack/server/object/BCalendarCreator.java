package com.tridium.bacnet.stack.server.object;

import com.tridium.bacnet.asn.NErrorType;
import com.tridium.bacnet.stack.server.BBacnetExportTable;
import javax.baja.bacnet.BacnetException;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.export.BBacnetCalendarDescriptor;
import javax.baja.bacnet.export.BIBacnetExportObject;
import javax.baja.bacnet.io.ErrorType;
import javax.baja.bacnet.io.PropertyValue;
import javax.baja.naming.BOrd;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.schedule.BCalendarSchedule;
import javax.baja.sys.BComponent;
import javax.baja.sys.BString;
import javax.baja.sys.BValue;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public class BCalendarCreator extends BBacnetObjectCreator {
   public static final Type TYPE = Sys.loadType(BCalendarCreator.class);
   private static int[] SUPPORTED_INITIAL_PROPERTY = new int[]{77, 28, 23};
   private static int[] SUPPORTED_PROPERTY = new int[]{77, 28, 23, 75, 79, 85, 371};

   @Override
   public Type getType() {
      return TYPE;
   }

   @Override
   public boolean isObjectTypeSupported(int objectType) {
      return objectType == 6;
   }

   @Override
   public BIBacnetExportObject createObject(BBacnetObjectIdentifier oid) {
      BBacnetCalendarDescriptor descriptor = new BBacnetCalendarDescriptor();
      descriptor.setObjectId(oid);
      BCalendarSchedule calendar = new BCalendarSchedule();
      descriptor.add("DynamicCalendar", calendar);
      calendar.setCleanupExpiredEvents(false);
      return descriptor;
   }

   @Override
   public ErrorType writeInitialValue(BIBacnetExportObject exportDescriptor, PropertyValue propertyValue) throws BacnetException {
      switch (propertyValue.getPropertyId()) {
         case 23:
         case 28:
         case 77:
            return exportDescriptor.writeProperty(propertyValue);
         default:
            return new NErrorType(2, 40);
      }
   }

   @Override
   public ErrorType exportObject(BBacnetExportTable table, BIBacnetExportObject exportDescriptor) {
      try {
         if (exportDescriptor instanceof BValue) {
            BValue ed = (BValue)exportDescriptor;
            BComponent dynafolder = table.get("dynamicObjects").asComponent();
            String expDescName = "calendar_" + exportDescriptor.getObjectId().getInstanceNumber();
            dynafolder.add(expDescName, ed);
            BOrd b = ((BCalendarSchedule)((BBacnetCalendarDescriptor)dynafolder.get(expDescName)).get("DynamicCalendar")).getHandleOrd();
            BBacnetCalendarDescriptor bcd = (BBacnetCalendarDescriptor)dynafolder.get(expDescName);
            bcd.setCalendarOrd(b);
            bcd.set("objectName", BString.make(expDescName));
            bcd.add("dynamic", BString.make("dynamic"), 5);
            return null;
         } else {
            BacObjCreatorDeletorUtil.logger.warning("dynamically created BIBacnetExportObjects must extend BValue");
            return null;
         }
      } catch (Exception var8) {
         BacObjCreatorDeletorUtil.logger.warning(var8.getMessage());
         return new NErrorType(2, 56);
      }
   }

   @Override
   protected int[] getSupportedInitialValue() {
      return SUPPORTED_INITIAL_PROPERTY;
   }

   @Override
   protected int[] getSupportedProperty() {
      return SUPPORTED_PROPERTY;
   }
}
