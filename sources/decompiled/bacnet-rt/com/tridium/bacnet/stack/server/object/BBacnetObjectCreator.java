package com.tridium.bacnet.stack.server.object;

import com.tridium.bacnet.stack.server.BBacnetExportTable;
import javax.baja.bacnet.BacnetException;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.export.BIBacnetExportObject;
import javax.baja.bacnet.io.ErrorType;
import javax.baja.bacnet.io.PropertyValue;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BComponent;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public abstract class BBacnetObjectCreator extends BComponent {
   public static final Type TYPE = Sys.loadType(BBacnetObjectCreator.class);

   public Type getType() {
      return TYPE;
   }

   public abstract boolean isObjectTypeSupported(int var1);

   public abstract BIBacnetExportObject createObject(BBacnetObjectIdentifier var1);

   public abstract ErrorType writeInitialValue(BIBacnetExportObject var1, PropertyValue var2) throws BacnetException;

   public abstract ErrorType exportObject(BBacnetExportTable var1, BIBacnetExportObject var2);

   protected abstract int[] getSupportedInitialValue();

   protected abstract int[] getSupportedProperty();

   public final boolean isInitialValueSupported(int propertyId) {
      return this.checkIdInArray(propertyId, this.getSupportedInitialValue());
   }

   public final boolean checkProperties(int propertyId) {
      return this.checkIdInArray(propertyId, this.getSupportedProperty());
   }

   private boolean checkIdInArray(int id, int[] array) {
      for (int i = 0; i < array.length; i++) {
         if (id == array[i]) {
            return true;
         }
      }

      return false;
   }

   public boolean shouldExport() {
      return true;
   }

   public void postProcess(BIBacnetExportObject expDesc) {
   }
}
