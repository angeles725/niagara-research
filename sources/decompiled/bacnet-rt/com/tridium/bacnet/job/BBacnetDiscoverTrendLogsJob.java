package com.tridium.bacnet.job;

import com.tridium.bacnet.asn.AsnUtil;
import com.tridium.bacnet.history.BBacnetHistoryDeviceExt;
import com.tridium.bacnet.history.BacnetTrendLogUtil;
import java.util.logging.Level;
import javax.baja.bacnet.datatypes.BBacnetDeviceObjectPropertyReference;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.util.PropertyInfo;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.util.BTypeSpec;
import javax.baja.util.Lexicon;

@NiagaraType
public class BBacnetDiscoverTrendLogsJob extends BBacnetDiscoverJob {
   public static final Type TYPE = Sys.loadType(BBacnetDiscoverTrendLogsJob.class);
   private static final Lexicon lex = Lexicon.make("bacnet");
   private static final String HISTORY_TYPE_UNKNOWN = lex.getText("historyType.unknown");

   @Override
   public Type getType() {
      return TYPE;
   }

   public BBacnetDiscoverTrendLogsJob() {
   }

   public BBacnetDiscoverTrendLogsJob(BBacnetHistoryDeviceExt deviceExt) {
      super(deviceExt);
   }

   @Override
   protected boolean doForId(BBacnetObjectIdentifier objectId) {
      return objectId.getObjectType() == 20 || objectId.getObjectType() == 27;
   }

   @Override
   int[] getDiscoveryPropIds(BBacnetObjectIdentifier objectId) {
      return new int[]{132, 28};
   }

   @Override
   void addDiscoveryChild(BBacnetDiscoverJob.IdVals iv) {
      String ht = this.findHistoryType(iv);
      BDiscoveryLog dl = new BDiscoveryLog(iv.name, iv.id, ht);
      BBacnetDiscoverJob.PropVal desc = iv.get(28);
      if (desc != null) {
         dl.setDescription(desc.toString());
      }

      this.add(null, dl);
   }

   private String findHistoryType(BBacnetDiscoverJob.IdVals iv) {
      String historyType = null;

      try {
         BBacnetDiscoverJob.PropVal logDevObjProp = iv.get(132);
         BBacnetDeviceObjectPropertyReference logDevObjPropVal = null;
         if (logDevObjProp != null) {
            if (logger.isLoggable(Level.FINE)) {
               logger.fine("findHistoryType: ldop=" + logDevObjProp.dbg());
            }

            if (!logDevObjProp.err) {
               logDevObjPropVal = (BBacnetDeviceObjectPropertyReference)logDevObjProp.val;
            }
         }

         if (logDevObjPropVal != null && isObjectConfigured(logDevObjPropVal)) {
            historyType = getHistoryType(logDevObjPropVal);
         }

         if (historyType == null && iv.id != null) {
            BTypeSpec recordTypeSpec = BacnetTrendLogUtil.findHistoryTypeByRecords(this.device, iv.id);
            if (recordTypeSpec != null) {
               String recordType = recordTypeSpec.getTypeName();
               historyType = recordType.substring(6, recordType.length() - 11);
            }
         }

         if (historyType == null && logDevObjPropVal != null && isObjectConfigured(logDevObjPropVal)) {
            historyType = this.guessHistoryTypeByAsn(logDevObjPropVal);
         }
      } catch (Exception var7) {
         this.log().message("Unable to determine history type for history " + iv);
      }

      return historyType != null ? historyType : HISTORY_TYPE_UNKNOWN;
   }

   private static String getHistoryType(BBacnetDeviceObjectPropertyReference dop) {
      int objectType = dop.getObjectId().getObjectType();
      int propertyId = dop.getPropertyId();
      switch (objectType) {
         case 0:
         case 1:
         case 2:
            switch (propertyId) {
               case 85:
                  return "Numeric";
               default:
                  return null;
            }
         case 3:
         case 4:
         case 5:
            switch (propertyId) {
               case 85:
                  return "Boolean";
            }
         case 6:
         case 7:
         case 8:
         case 9:
         case 10:
         case 11:
         case 12:
         case 15:
         case 16:
         case 17:
         case 18:
         default:
            break;
         case 13:
         case 14:
         case 19:
            switch (propertyId) {
               case 85:
                  return "Enum";
            }
      }

      return null;
   }

   private String guessHistoryTypeByAsn(BBacnetDeviceObjectPropertyReference dop) {
      int objectType = dop.getObjectId().getObjectType();
      int propertyId = dop.getPropertyId();
      PropertyInfo pi = this.device.getPropertyInfo(objectType, propertyId);
      if (pi != null) {
         int asnType = pi.getAsnType();
         if (asnType == -2) {
            if (dop.getPropertyArrayIndex() == -1) {
               return "String";
            }

            String typespec = pi.getType();
            Type t = BTypeSpec.make(typespec).getResolvedType();
            asnType = AsnUtil.getAsnType(t);
         }

         return asnTypeToHistoryRecordType(asnType);
      } else {
         return "String";
      }
   }

   private static String asnTypeToHistoryRecordType(int asnType) {
      switch (asnType) {
         case 0:
            return null;
         case 1:
            return "Boolean";
         case 2:
            return "Enum";
         case 3:
            return "Enum";
         case 4:
            return "Numeric";
         case 5:
            return "Numeric";
         case 6:
            return "String";
         case 7:
            return "String";
         case 8:
            return "String";
         case 9:
            return "Enum";
         case 10:
            return "String";
         case 11:
            return "String";
         case 12:
            return "String";
         default:
            return "String";
      }
   }

   private static boolean isObjectConfigured(BBacnetDeviceObjectPropertyReference objPropRef) {
      int instanceNumber = objPropRef.getObjectId().getInstanceNumber();
      return instanceNumber >= 0 && instanceNumber < 4194303;
   }
}
