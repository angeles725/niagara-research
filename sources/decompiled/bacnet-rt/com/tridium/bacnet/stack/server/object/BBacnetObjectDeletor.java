package com.tridium.bacnet.stack.server.object;

import com.tridium.bacnet.asn.NErrorType;
import com.tridium.bacnet.stack.server.BBacnetExportTable;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.export.BIBacnetExportObject;
import javax.baja.bacnet.io.ErrorType;
import javax.baja.naming.BOrd;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BComplex;
import javax.baja.sys.BComponent;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public abstract class BBacnetObjectDeletor extends BComponent {
   public static final Type TYPE = Sys.loadType(BBacnetObjectDeletor.class);

   public Type getType() {
      return TYPE;
   }

   public abstract boolean isObjectTypeSupported(int var1);

   public BOrd getRemoteExtensionToDelete(BIBacnetExportObject descriptor) {
      return descriptor.getObjectOrd();
   }

   public ErrorType deleteObject(BBacnetObjectIdentifier oid, BBacnetExportTable exportTable) {
      BIBacnetExportObject expObject = exportTable.byObjectId(oid);
      BOrd remoteOrd = this.getRemoteExtensionToDelete(expObject);
      BComplex descriptor = (BComplex)expObject;
      if (this.isObjectDeletable(exportTable, descriptor)) {
         exportTable.get("dynamicObjects").asComponent().remove(descriptor.asComplex());
         if (remoteOrd != null && !remoteOrd.isNull()) {
            try {
               BComponent remoteExt = (BComponent)remoteOrd.get(exportTable);
               ((BComponent)remoteExt.getParent()).remove(remoteExt);
            } catch (Exception var7) {
               BacObjCreatorDeletorUtil.logger.info("Error while deleting Remote extension. " + var7.getMessage());
            }
         }

         return null;
      } else {
         return new NErrorType(1, 23);
      }
   }

   private boolean isObjectDeletable(BBacnetExportTable exportTable, BComplex desc) {
      try {
         return exportTable.get("dynamicObjects").asComponent().getProperty(desc.getName()) != null;
      } catch (Exception var4) {
         return false;
      }
   }
}
