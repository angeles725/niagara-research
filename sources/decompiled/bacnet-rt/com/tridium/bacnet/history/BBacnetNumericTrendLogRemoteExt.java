package com.tridium.bacnet.history;

import javax.baja.bacnet.BBacnetDevice;
import javax.baja.bacnet.datatypes.BBacnetNull;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.datatypes.BBacnetUnsigned;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.status.BStatusNumeric;
import javax.baja.status.BStatusValue;
import javax.baja.sys.BFloat;
import javax.baja.sys.BInteger;
import javax.baja.sys.BValue;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public class BBacnetNumericTrendLogRemoteExt extends BBacnetTrendLogRemoteExt {
   public static final Type TYPE = Sys.loadType(BBacnetNumericTrendLogRemoteExt.class);
   private BBacnetNumericTrendRecord rec;

   @Override
   public Type getType() {
      return TYPE;
   }

   public BBacnetNumericTrendLogRemoteExt() {
   }

   public BBacnetNumericTrendLogRemoteExt(BBacnetDevice device, BBacnetObjectIdentifier objectId, int propertyId, int arrayIndex) {
      super(device, objectId, propertyId, arrayIndex);
   }

   @Override
   public Type getRecordType() {
      return BBacnetNumericTrendRecord.TYPE;
   }

   @Override
   public BBacnetTrendRecord getRecord() {
      return this.rec;
   }

   public void started() throws Exception {
      this.rec = new BBacnetNumericTrendRecord();
      super.started();
   }

   @Override
   protected BStatusValue getStatusValue(BValue value) {
      if (value instanceof BBacnetNull) {
         return null;
      } else if (value instanceof BInteger) {
         return new BStatusNumeric(((BInteger)value).getInt());
      } else {
         return value instanceof BBacnetUnsigned ? new BStatusNumeric(((BBacnetUnsigned)value).getLong()) : new BStatusNumeric(((BFloat)value).getFloat());
      }
   }

   @Override
   protected boolean areEqual(BStatusValue oldValue, BStatusValue newValue) {
      return oldValue == null || newValue != null && ((BStatusNumeric)oldValue).getNumeric() == ((BStatusNumeric)newValue).getNumeric();
   }
}
