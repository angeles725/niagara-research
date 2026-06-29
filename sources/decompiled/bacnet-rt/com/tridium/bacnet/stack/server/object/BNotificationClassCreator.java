package com.tridium.bacnet.stack.server.object;

import com.tridium.bacnet.asn.NErrorType;
import com.tridium.bacnet.stack.server.BBacnetExportTable;
import javax.baja.alarm.BAlarmClass;
import javax.baja.alarm.BAlarmService;
import javax.baja.bacnet.BacnetException;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.export.BBacnetNotificationClassDescriptor;
import javax.baja.bacnet.export.BIBacnetExportObject;
import javax.baja.bacnet.io.ErrorType;
import javax.baja.bacnet.io.PropertyValue;
import javax.baja.naming.BOrd;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BComponent;
import javax.baja.sys.BString;
import javax.baja.sys.BValue;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public class BNotificationClassCreator extends BBacnetObjectCreator {
   public static final Type TYPE = Sys.loadType(BNotificationClassCreator.class);
   private static int[] SUPPORTED_INITIAL_PROPERTY = new int[]{77, 28, 86, 1, 102, 17};
   private static int[] SUPPORTED_PROPERTY = new int[]{77, 28, 17, 75, 79, 86, 1, 102, 371};

   @Override
   public Type getType() {
      return TYPE;
   }

   @Override
   protected int[] getSupportedInitialValue() {
      return SUPPORTED_INITIAL_PROPERTY;
   }

   @Override
   protected int[] getSupportedProperty() {
      return SUPPORTED_PROPERTY;
   }

   @Override
   public boolean isObjectTypeSupported(int objectType) {
      return objectType == 15;
   }

   @Override
   public BIBacnetExportObject createObject(BBacnetObjectIdentifier oid) {
      BBacnetNotificationClassDescriptor descriptor = new BBacnetNotificationClassDescriptor();
      descriptor.setObjectId(oid);
      BAlarmService as = (BAlarmService)Sys.getService(BAlarmService.TYPE);
      BAlarmClass ac = new BAlarmClass();
      as.add("DynamicAlarmClass?", ac);
      descriptor.setAlarmClassOrd(ac.getSlotPathOrd());
      return descriptor;
   }

   @Override
   public ErrorType writeInitialValue(BIBacnetExportObject exportDescriptor, PropertyValue propertyValue) throws BacnetException {
      switch (propertyValue.getPropertyId()) {
         case 1:
         case 86:
            return exportDescriptor.writeProperty(propertyValue);
         case 28:
         case 77:
            try {
               return exportDescriptor.writeProperty(propertyValue);
            } catch (Exception var4) {
               return new NErrorType(2, 41);
            }
         case 102:
            return exportDescriptor.writeProperty(propertyValue);
         default:
            return new NErrorType(2, 40);
      }
   }

   @Override
   public ErrorType exportObject(BBacnetExportTable table, BIBacnetExportObject exportDescriptor) {
      if (exportDescriptor instanceof BValue) {
         BValue ed = (BValue)exportDescriptor;
         BComponent dynafolder = table.get("dynamicObjects").asComponent();
         String expDescName = "notificationClass_" + exportDescriptor.getObjectId().getInstanceNumber();
         dynafolder.add(expDescName, ed);
         BOrd bOrd = ((BBacnetNotificationClassDescriptor)dynafolder.get(expDescName)).getAlarmClassOrd();
         BBacnetNotificationClassDescriptor bnd = (BBacnetNotificationClassDescriptor)dynafolder.get(expDescName);
         bnd.set("alarmClassOrd", bOrd);
         bnd.set("objectName", BString.make(expDescName));
         bnd.add("dynamic", BString.make("dynamic"), 5);
      } else {
         BacObjCreatorDeletorUtil.logger.warning("dynamically created BIBacnetExportObjects must extend BValue");
      }

      return null;
   }
}
