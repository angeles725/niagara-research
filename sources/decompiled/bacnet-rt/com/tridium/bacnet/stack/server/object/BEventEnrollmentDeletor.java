package com.tridium.bacnet.stack.server.object;

import com.tridium.bacnet.stack.server.BBacnetExportTable;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.export.BBacnetEventEnrollmentDescriptor;
import javax.baja.bacnet.export.BIBacnetExportObject;
import javax.baja.bacnet.io.ErrorType;
import javax.baja.naming.BOrd;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BComplex;
import javax.baja.sys.BComponent;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public class BEventEnrollmentDeletor extends BBacnetObjectDeletor {
   public static final Type TYPE = Sys.loadType(BEventEnrollmentDeletor.class);

   @Override
   public Type getType() {
      return TYPE;
   }

   @Override
   public boolean isObjectTypeSupported(int objectType) {
      return objectType == 9;
   }

   @Override
   public BOrd getRemoteExtensionToDelete(BIBacnetExportObject desc) {
      return ((BBacnetEventEnrollmentDescriptor)desc).getEventEnrollmentOrd();
   }

   @Override
   public ErrorType deleteObject(BBacnetObjectIdentifier oid, BBacnetExportTable exportTable) {
      BBacnetEventEnrollmentDescriptor eed = (BBacnetEventEnrollmentDescriptor)exportTable.byObjectId(oid);
      if (eed.getObjectPropertyReference().getPropertyId() == 33) {
         BComponent remoteExt = (BComponent)this.getRemoteExtensionToDelete(eed).get(exportTable);
         BComponent pointsFolder = (BComponent)remoteExt.getParent().getParent();
         BComplex extParent = remoteExt.getParent();
         pointsFolder.remove(extParent);
         eed.setEventEnrollmentOrd(BOrd.NULL);
      }

      return super.deleteObject(oid, exportTable);
   }
}
