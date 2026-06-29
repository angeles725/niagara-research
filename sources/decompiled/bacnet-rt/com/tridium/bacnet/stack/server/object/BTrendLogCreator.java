package com.tridium.bacnet.stack.server.object;

import com.tridium.bacnet.asn.NBacnetPropertyValue;
import com.tridium.bacnet.asn.NErrorType;
import com.tridium.bacnet.stack.server.BBacnetExportTable;
import javax.baja.bacnet.BBacnetNetwork;
import javax.baja.bacnet.BacnetException;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.export.BBacnetEventSource;
import javax.baja.bacnet.export.BBacnetTrendLogDescriptor;
import javax.baja.bacnet.export.BIBacnetExportObject;
import javax.baja.bacnet.export.BLocalBacnetDevice;
import javax.baja.bacnet.io.ErrorType;
import javax.baja.bacnet.io.PropertyValue;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BComponent;
import javax.baja.sys.BValue;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public class BTrendLogCreator extends BBacnetObjectCreator {
   public static final Type TYPE = Sys.loadType(BTrendLogCreator.class);
   public static final String TREND_LOG_DESC_NAME = "trendLog";
   private static int[] SUPPORTED_INITIAL_VALUES = new int[]{77, 28, 133, 142, 143, 132, 134, 128, 127, 144, 137, 17, 35, 72, 126};
   private static int[] SUPPORTED_VALUES = new int[]{
      77, 28, 132, 134, 17, 144, 133, 141, 35, 72, 137, 0, 130, 351, 140, 173, 353, 205, 142, 143, 127, 128, 126
   };

   @Override
   public Type getType() {
      return TYPE;
   }

   @Override
   public boolean isObjectTypeSupported(int objectType) {
      return objectType == 20;
   }

   @Override
   public BIBacnetExportObject createObject(BBacnetObjectIdentifier oid) {
      BIBacnetExportObject descriptor = new BBacnetTrendLogDescriptor();
      descriptor.setObjectId(oid);
      ((BBacnetEventSource)descriptor).setDynamicallyCreated(true);
      descriptor.setObjectName("TrendLog_" + oid.getInstanceNumber());
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
         BComponent dynamicFolder = table.get("dynamicObjects").asComponent();
         String expDescName = "trendLog" + exportDescriptor.getObjectId().getInstanceNumber();
         if (exportDescriptor instanceof BValue) {
            dynamicFolder.add(expDescName, (BValue)exportDescriptor);
         }

         BLocalBacnetDevice local = BBacnetNetwork.localDevice();
         local.export(exportDescriptor);
         return null;
      } catch (IllegalArgumentException var6) {
         return new NErrorType(2, 80);
      } catch (Exception var7) {
         return new NErrorType(2, 56);
      }
   }

   @Override
   protected int[] getSupportedInitialValue() {
      return SUPPORTED_INITIAL_VALUES;
   }

   @Override
   protected int[] getSupportedProperty() {
      return SUPPORTED_VALUES;
   }
}
