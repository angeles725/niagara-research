package com.tridium.bacnet.stack.server.object;

import com.tridium.bacnet.asn.AsnInputStream;
import com.tridium.bacnet.asn.AsnUtil;
import com.tridium.bacnet.asn.NErrorType;
import com.tridium.bacnet.schedule.ScheduleSupport0;
import com.tridium.bacnet.schedule.ScheduleType;
import com.tridium.bacnet.stack.server.BBacnetExportTable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.baja.bacnet.BacnetException;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.datatypes.BBacnetObjectPropertyReference;
import javax.baja.bacnet.export.BBacnetBooleanScheduleDescriptor;
import javax.baja.bacnet.export.BBacnetDynamicScheduleDescriptor;
import javax.baja.bacnet.export.BBacnetEnumScheduleDescriptor;
import javax.baja.bacnet.export.BBacnetNumericScheduleDescriptor;
import javax.baja.bacnet.export.BBacnetScheduleDescriptor;
import javax.baja.bacnet.export.BBacnetStringScheduleDescriptor;
import javax.baja.bacnet.export.BIBacnetExportObject;
import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.io.ErrorType;
import javax.baja.bacnet.io.PropertyValue;
import javax.baja.naming.BOrd;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.util.Array;
import javax.baja.nre.util.ByteArrayUtil;
import javax.baja.schedule.BBooleanSchedule;
import javax.baja.schedule.BEnumSchedule;
import javax.baja.schedule.BNumericSchedule;
import javax.baja.schedule.BStringSchedule;
import javax.baja.schedule.BWeeklySchedule;
import javax.baja.status.BStatus;
import javax.baja.status.BStatusValue;
import javax.baja.sys.BComponent;
import javax.baja.sys.BString;
import javax.baja.sys.BValue;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public class BScheduleCreator extends BBacnetObjectCreator {
   public static final Type TYPE = Sys.loadType(BScheduleCreator.class);
   private static int[] SUPPORTED_INITIAL_PROPERTY = new int[]{77, 32, 123, 38, 54, 88, 81, 28, 85, 174};
   private static int[] SUPPORTED_PROPERTY = new int[]{75, 79, 371, 77, 32, 123, 38, 54, 88, 111, 103, 81, 28, 85, 174};

   @Override
   public Type getType() {
      return TYPE;
   }

   @Override
   public boolean isObjectTypeSupported(int objectType) {
      return objectType == 17;
   }

   @Override
   protected int[] getSupportedInitialValue() {
      return SUPPORTED_INITIAL_PROPERTY;
   }

   @Override
   protected int[] getSupportedProperty() {
      return SUPPORTED_PROPERTY;
   }

   public BIBacnetExportObject createObject(BBacnetObjectIdentifier oid, Array<PropertyValue> initialValues) throws Exception {
      int type = -1;
      if (initialValues != null) {
         for (int i = 0; i < initialValues.size(); i++) {
            PropertyValue pv = (PropertyValue)initialValues.get(i);
            switch (pv.getPropertyId()) {
               case 38:
                  type = this.getScheduleTypeFromExceptionSched(pv);
                  break;
               case 54:
                  type = this.getScheduleTypeFromListOfObjRef(pv);
                  break;
               case 85:
               case 174:
                  type = this.getScheduleTypeFromPvSd(pv);
                  break;
               case 123:
                  type = this.getScheduleTypeFromWeeklySched(pv);
            }

            if (type != -1) {
               break;
            }
         }
      }

      return createTypedSchedule(type, oid);
   }

   private int getScheduleTypeFromListOfObjRef(PropertyValue pv) throws Exception {
      try {
         AsnInputStream asn = new AsnInputStream();
         asn.setBuffer(pv.getPropertyValue());
         BBacnetObjectPropertyReference bObjRef = new BBacnetObjectPropertyReference();
         bObjRef.readAsn(asn);
         int objectType = bObjRef.getObjectId().getObjectType();
         int propId = bObjRef.getPropertyId();
         return BacObjCreatorDeletorUtil.scheduleTypeFromRef(objectType, propId);
      } catch (Exception var7) {
         BacObjCreatorDeletorUtil.logger.severe("Error while reading Initial Property: " + var7.getMessage());
         throw var7;
      }
   }

   private int getScheduleTypeFromExceptionSched(PropertyValue pv) throws AsnException {
      int type = -1;

      try {
         AsnInputStream asnIn = new AsnInputStream();
         Pattern r = Pattern.compile("2e[\\s\\S]*?2f");
         Matcher m = r.matcher(ByteArrayUtil.toHexString(pv.getPropertyValue()));

         while (m.find()) {
            asnIn.setBuffer(ByteArrayUtil.hexStringToBytes(m.group()));
            ScheduleType st = new ScheduleSupport0().getScheduleType(2, asnIn);
            if (st != null) {
               type = BacObjCreatorDeletorUtil.getScheduleType(st);
            }
         }

         return type;
      } catch (AsnException var7) {
         BacObjCreatorDeletorUtil.logger.severe("Error while reading Initial Property: " + var7.getMessage());
         throw var7;
      }
   }

   private int getScheduleTypeFromWeeklySched(PropertyValue pv) throws AsnException {
      int type = -1;

      try {
         AsnInputStream asnIn = new AsnInputStream();
         String[] week = ByteArrayUtil.toHexString(pv.getPropertyValue()).split("0f");

         for (int day = 0; day < week.length; day++) {
            asnIn.setBuffer(ByteArrayUtil.hexStringToBytes(week[day] + "0f"));
            ScheduleType st = new ScheduleSupport0().getScheduleType(0, asnIn);
            if (st != null) {
               type = BacObjCreatorDeletorUtil.getScheduleType(st);
               break;
            }
         }

         return type;
      } catch (AsnException var7) {
         BacObjCreatorDeletorUtil.logger.severe("Error while reading Initial Property: " + var7.getMessage());
         throw var7;
      }
   }

   private int getScheduleTypeFromPvSd(PropertyValue pv) throws AsnException {
      int type = -1;

      try {
         return BacObjCreatorDeletorUtil.getScheduleType(AsnUtil.getAsnType(pv.getPropertyValue()));
      } catch (AsnException var4) {
         BacObjCreatorDeletorUtil.logger.severe("Error while reading Initial Property:" + var4.getMessage());
         throw var4;
      }
   }

   private static BIBacnetExportObject createTypedSchedule(int type, BBacnetObjectIdentifier oid) {
      BBacnetScheduleDescriptor bacnetSchdDesc;
      BWeeklySchedule schedule;
      switch (type) {
         case 0:
            bacnetSchdDesc = new BBacnetNumericScheduleDescriptor();
            schedule = new BNumericSchedule();
            break;
         case 1:
            bacnetSchdDesc = new BBacnetStringScheduleDescriptor();
            schedule = new BStringSchedule();
            break;
         case 2:
            bacnetSchdDesc = new BBacnetEnumScheduleDescriptor();
            schedule = new BEnumSchedule();
            break;
         case 3:
            bacnetSchdDesc = new BBacnetBooleanScheduleDescriptor();
            schedule = new BBooleanSchedule();
            break;
         default:
            bacnetSchdDesc = new BBacnetDynamicScheduleDescriptor();
            schedule = new BNumericSchedule();
      }

      ((BStatusValue)schedule.get("out")).setStatus(17);
      bacnetSchdDesc.add("DynamicSchedule", schedule);
      bacnetSchdDesc.setObjectId(oid);
      bacnetSchdDesc.setObjectName("schedule_" + oid.getInstanceNumber());
      schedule.setCleanupExpiredEvents(false);
      return bacnetSchdDesc;
   }

   @Override
   public ErrorType writeInitialValue(BIBacnetExportObject exportDescriptor, PropertyValue propertyValue) throws BacnetException {
      switch (propertyValue.getPropertyId()) {
         case 28:
         case 32:
         case 38:
         case 54:
         case 77:
         case 81:
         case 85:
         case 88:
         case 123:
         case 174:
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
            String expDescName = "schedule_" + exportDescriptor.getObjectId().getInstanceNumber();
            dynafolder.add(expDescName, ed);
            BBacnetScheduleDescriptor bcd = (BBacnetScheduleDescriptor)dynafolder.get(expDescName);
            bcd.add("dynamic", BString.make("dynamic"), 5);
            BOrd b = ((BWeeklySchedule)((BBacnetScheduleDescriptor)dynafolder.get(expDescName)).get("DynamicSchedule")).getHandleOrd();
            bcd.setScheduleOrd(b);
            ((BStatusValue)((BWeeklySchedule)((BBacnetScheduleDescriptor)dynafolder.get(expDescName)).get("DynamicSchedule")).get("out"))
               .setStatus(BStatus.disabled);
            return null;
         } else {
            BacObjCreatorDeletorUtil.logger.warning("Dynamically created BIBacnetExportObjects must extend BValue");
            return null;
         }
      } catch (Exception var8) {
         BacObjCreatorDeletorUtil.logger.warning("Dynamic Schedule creation - Error while exporting Object." + var8.getCause());
         return new NErrorType(2, 56);
      }
   }

   @Override
   public BIBacnetExportObject createObject(BBacnetObjectIdentifier oid) {
      return null;
   }
}
