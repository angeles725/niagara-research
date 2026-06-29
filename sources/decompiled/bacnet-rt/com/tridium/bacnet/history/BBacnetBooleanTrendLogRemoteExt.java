package com.tridium.bacnet.history;

import javax.baja.bacnet.BBacnetDevice;
import javax.baja.bacnet.datatypes.BBacnetNull;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.status.BStatusBoolean;
import javax.baja.status.BStatusValue;
import javax.baja.sys.BBoolean;
import javax.baja.sys.BDynamicEnum;
import javax.baja.sys.BValue;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public class BBacnetBooleanTrendLogRemoteExt extends BBacnetTrendLogRemoteExt {
   public static final Type TYPE = Sys.loadType(BBacnetBooleanTrendLogRemoteExt.class);
   private BBacnetBooleanTrendRecord rec;

   @Override
   public Type getType() {
      return TYPE;
   }

   public BBacnetBooleanTrendLogRemoteExt() {
   }

   public BBacnetBooleanTrendLogRemoteExt(BBacnetDevice device, BBacnetObjectIdentifier objectId, int propertyId, int arrayIndex) {
      super(device, objectId, propertyId, arrayIndex);
   }

   @Override
   public Type getRecordType() {
      return BBacnetBooleanTrendRecord.TYPE;
   }

   @Override
   public BBacnetTrendRecord getRecord() {
      return this.rec;
   }

   public void started() throws Exception {
      this.rec = new BBacnetBooleanTrendRecord();
      super.started();
   }

   @Override
   protected BStatusValue getStatusValue(BValue value) {
      if (value instanceof BBacnetNull) {
         return null;
      } else {
         boolean v;
         if (value instanceof BBoolean) {
            v = ((BBoolean)value).getBoolean();
         } else {
            v = ((BDynamicEnum)value).getOrdinal() != 0;
         }

         return new BStatusBoolean(v);
      }
   }

   @Override
   protected boolean areEqual(BStatusValue oldValue, BStatusValue newValue) {
      return oldValue == null || newValue != null && ((BStatusBoolean)oldValue).getBoolean() == ((BStatusBoolean)newValue).getBoolean();
   }
}
