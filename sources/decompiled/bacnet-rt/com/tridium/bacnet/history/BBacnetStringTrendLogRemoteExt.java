package com.tridium.bacnet.history;

import javax.baja.bacnet.BBacnetDevice;
import javax.baja.bacnet.datatypes.BBacnetNull;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.status.BStatusString;
import javax.baja.status.BStatusValue;
import javax.baja.sys.BString;
import javax.baja.sys.BValue;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public class BBacnetStringTrendLogRemoteExt extends BBacnetTrendLogRemoteExt {
   public static final Type TYPE = Sys.loadType(BBacnetStringTrendLogRemoteExt.class);
   private BBacnetStringTrendRecord rec;

   @Override
   public Type getType() {
      return TYPE;
   }

   public BBacnetStringTrendLogRemoteExt() {
   }

   public BBacnetStringTrendLogRemoteExt(BBacnetDevice device, BBacnetObjectIdentifier objectId, int propertyId, int arrayIndex) {
      super(device, objectId, propertyId, arrayIndex);
   }

   @Override
   public Type getRecordType() {
      return BBacnetStringTrendRecord.TYPE;
   }

   @Override
   public BBacnetTrendRecord getRecord() {
      return this.rec;
   }

   public void started() throws Exception {
      this.rec = new BBacnetStringTrendRecord();
      super.started();
   }

   @Override
   protected BStatusValue getStatusValue(BValue value) {
      if (value instanceof BBacnetNull) {
         return null;
      } else {
         String v = ((BString)value).getString();
         return new BStatusString(v);
      }
   }

   @Override
   protected boolean areEqual(BStatusValue oldValue, BStatusValue newValue) {
      return oldValue == null || newValue != null && ((BStatusString)oldValue).getValue().equals(((BStatusString)newValue).getValue());
   }
}
