package com.tridium.bacnet.history;

import javax.baja.bacnet.BBacnetDevice;
import javax.baja.bacnet.datatypes.BBacnetNull;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.datatypes.BBacnetUnsigned;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.status.BStatusEnum;
import javax.baja.status.BStatusValue;
import javax.baja.sys.BDynamicEnum;
import javax.baja.sys.BValue;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public class BBacnetEnumTrendLogRemoteExt extends BBacnetTrendLogRemoteExt {
   public static final Type TYPE = Sys.loadType(BBacnetEnumTrendLogRemoteExt.class);
   private BBacnetEnumTrendRecord rec;

   @Override
   public Type getType() {
      return TYPE;
   }

   public BBacnetEnumTrendLogRemoteExt() {
   }

   public BBacnetEnumTrendLogRemoteExt(BBacnetDevice device, BBacnetObjectIdentifier objectId, int propertyId, int arrayIndex) {
      super(device, objectId, propertyId, arrayIndex);
   }

   @Override
   public Type getRecordType() {
      return BBacnetEnumTrendRecord.TYPE;
   }

   @Override
   public BBacnetTrendRecord getRecord() {
      return this.rec;
   }

   public void started() throws Exception {
      this.rec = new BBacnetEnumTrendRecord();
      super.started();
   }

   @Override
   protected BStatusValue getStatusValue(BValue value) {
      if (value instanceof BBacnetNull) {
         return null;
      } else {
         int ordinal = 0;
         if (value instanceof BBacnetUnsigned) {
            ordinal = ((BBacnetUnsigned)value).getInt();
         }

         return new BStatusEnum(BDynamicEnum.make(ordinal));
      }
   }

   @Override
   protected boolean areEqual(BStatusValue oldValue, BStatusValue newValue) {
      return oldValue == null || newValue != null && ((BStatusEnum)oldValue).getEnum() == ((BStatusEnum)newValue).getEnum();
   }
}
