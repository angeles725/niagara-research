package com.tridium.bacnet.history;

import javax.baja.bacnet.BBacnetDevice;
import javax.baja.bacnet.datatypes.BBacnetBitString;
import javax.baja.bacnet.datatypes.BBacnetNull;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.status.BStatusNumeric;
import javax.baja.status.BStatusValue;
import javax.baja.sys.BValue;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public class BBacnetBitStringTrendLogRemoteExt extends BBacnetTrendLogRemoteExt {
   public static final Type TYPE = Sys.loadType(BBacnetBitStringTrendLogRemoteExt.class);
   private BBacnetBitStringTrendRecord rec;

   @Override
   public Type getType() {
      return TYPE;
   }

   public BBacnetBitStringTrendLogRemoteExt() {
   }

   public BBacnetBitStringTrendLogRemoteExt(BBacnetDevice device, BBacnetObjectIdentifier objectId, int propertyId, int arrayIndex) {
      super(device, objectId, propertyId, arrayIndex);
   }

   @Override
   public Type getRecordType() {
      return BBacnetBitStringTrendRecord.TYPE;
   }

   @Override
   public BBacnetTrendRecord getRecord() {
      return this.rec;
   }

   public void started() throws Exception {
      this.rec = new BBacnetBitStringTrendRecord();
      super.started();
      this.setFlags(precision, 4);
      this.setFlags(minRolloverValue, 4);
      this.setFlags(maxRolloverValue, 4);
   }

   @Override
   protected BStatusValue getStatusValue(BValue value) {
      if (value instanceof BBacnetNull) {
         return null;
      } else if (value instanceof BBacnetBitString) {
         BBacnetBitString bs = (BBacnetBitString)value;
         long bits = BBacnetBitStringTrendRecord.getBits(bs);
         return new BStatusNumeric(bits);
      } else {
         return new BStatusNumeric(0.0);
      }
   }

   @Override
   protected boolean areEqual(BStatusValue oldValue, BStatusValue newValue) {
      return oldValue == null || (long)((BStatusNumeric)oldValue).getNumeric() == (long)((BStatusNumeric)newValue).getNumeric();
   }
}
