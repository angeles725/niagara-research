package com.tridium.bacnet.stack.server.object;

import com.tridium.bacnet.asn.NBacnetPropertyValue;
import com.tridium.bacnet.asn.NErrorType;
import com.tridium.bacnet.stack.server.BBacnetExportTable;
import java.util.logging.Level;
import javax.baja.bacnet.BBacnetNetwork;
import javax.baja.bacnet.BacnetException;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.export.BBacnetEventEnrollmentDescriptor;
import javax.baja.bacnet.export.BIBacnetExportObject;
import javax.baja.bacnet.io.ErrorType;
import javax.baja.bacnet.io.PropertyValue;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BComponent;
import javax.baja.sys.BValue;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public class BEventEnrollmentCreator extends BBacnetObjectCreator {
   public static final Type TYPE = Sys.loadType(BEventEnrollmentCreator.class);
   private static final int[] SUPPORTED_INITIAL_PROPERTY = new int[]{77, 28, 72, 83, 78, 35, 17, 353};
   private static final int[] SUPPORTED_PROPERTY = new int[]{75, 77, 79, 28, 37, 72, 83, 78, 35, 17, 85, 371};
   public static final String EVENT_ENROLLMENT_DESC_NAME = "eventEnrollment";

   @Override
   public Type getType() {
      return TYPE;
   }

   @Override
   public boolean isObjectTypeSupported(int objectType) {
      return objectType == 9;
   }

   @Override
   public BIBacnetExportObject createObject(BBacnetObjectIdentifier oid) {
      BIBacnetExportObject descriptor = new BBacnetEventEnrollmentDescriptor();
      descriptor.setObjectId(oid);
      descriptor.setObjectName("EventEnrollment_" + oid.getInstanceNumber());
      return descriptor;
   }

   @Override
   public ErrorType writeInitialValue(BIBacnetExportObject exportDescriptor, PropertyValue propertyValue) throws BacnetException {
      byte[] pv = propertyValue.getPropertyValue();
      int pId = propertyValue.getPropertyId();
      return exportDescriptor.writeProperty(new NBacnetPropertyValue(pId, pv));
   }

   @Override
   public ErrorType exportObject(BBacnetExportTable table, BIBacnetExportObject exportDescriptor) {
      try {
         if (exportDescriptor instanceof BValue) {
            BComponent dynamicObjsFolder = table.get("dynamicObjects").asComponent();
            String expDescName = "eventEnrollment_" + exportDescriptor.getObjectId().getInstanceNumber();
            dynamicObjsFolder.add(expDescName, (BValue)exportDescriptor);
         } else if (BacObjCreatorDeletorUtil.logger.isLoggable(Level.FINE)) {
            BacObjCreatorDeletorUtil.logger.fine("EventEnrollmentCreator#exportObject: exportDescriptor is not a BValue; type: " + exportDescriptor.getType());
         }

         BBacnetNetwork.localDevice().export(exportDescriptor);
         return null;
      } catch (IllegalArgumentException var5) {
         if (BacObjCreatorDeletorUtil.logger.isLoggable(Level.FINE)) {
            BacObjCreatorDeletorUtil.logger.log(Level.FINE, "IllegalArgumentException exporting EventEnrollmentObject", (Throwable)var5);
         }

         return new NErrorType(2, 80);
      } catch (Exception var6) {
         if (BacObjCreatorDeletorUtil.logger.isLoggable(Level.FINE)) {
            BacObjCreatorDeletorUtil.logger.log(Level.FINE, "Exception exporting EventEnrollmentObject", (Throwable)var6);
         }

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

   @Override
   public void postProcess(BIBacnetExportObject descriptor) {
   }
}
