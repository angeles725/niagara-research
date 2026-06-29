package com.tridium.bacnet.stack.server;

import com.tridium.history.BHistory;
import com.tridium.json.JSONObject;
import java.util.Map;
import javax.baja.bacnet.export.BBacnetPointDescriptor;
import javax.baja.bacnet.export.BIBacnetExportObject;
import javax.baja.naming.BOrd;
import javax.baja.naming.OrdTarget;
import javax.baja.naming.SlotPath;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.rpc.NiagaraRpc;
import javax.baja.rpc.Transport;
import javax.baja.rpc.TransportType;
import javax.baja.security.BIProtected;
import javax.baja.sys.BComponent;
import javax.baja.sys.BInterface;
import javax.baja.sys.BObject;
import javax.baja.sys.BValue;
import javax.baja.sys.Context;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public interface BIBacnetExportFolder extends BInterface {
   Type TYPE = Sys.loadType(BIBacnetExportFolder.class);

   BBacnetExportTable getExports();

   String getName();

   @NiagaraRpc(
      permissions = "R",
      transports = {@Transport(
         type = TransportType.box
      )}
   )
   default JSONObject getExportData(String slotPathBody, Map<String, Object> params, Context context) {
      JSONObject exportData = new JSONObject();
      BValue value = (BValue)BOrd.make(this.getExports().getOrdInHost(), new SlotPath(slotPathBody)).get();
      if (null != value && value.getType().is(BIBacnetExportObject.TYPE)) {
         if (value instanceof BIProtected && !((BIProtected)value).getPermissions(context).hasOperatorRead()) {
            return exportData;
         }

         exportData.put("targetName", "");
         exportData.put("targetOutput", "null");
         BIBacnetExportObject exportObj = (BIBacnetExportObject)value;
         BOrd objectOrd = exportObj.getObjectOrd();
         exportData.put("objectOrd", objectOrd.toString(context));
         String writable = "";
         if (exportObj instanceof BBacnetPointDescriptor) {
            writable = ((BBacnetPointDescriptor)exportObj).getBacnetWritable();
         }

         exportData.put("bacnetWritable", writable);
         if (objectOrd.equals(BOrd.NULL)) {
            return exportData;
         }

         OrdTarget ordTarget = objectOrd.resolve(value, context);
         if (ordTarget.canRead()) {
            BObject targetObj = ordTarget.get();
            if (targetObj instanceof BComponent) {
               exportData.put("targetName", ((BComponent)targetObj).getName());
            } else if (targetObj instanceof BHistory) {
               exportData.put("targetName", ((BHistory)targetObj).getDisplayName(context));
            }

            exportData.put("targetOutput", targetObj.toString(context));
         }
      }

      return exportData;
   }

   default boolean operatorCanRead(BObject targetObj, Context context) {
      return targetObj instanceof BIProtected && ((BIProtected)targetObj).getPermissions(context).hasOperatorRead();
   }
}
