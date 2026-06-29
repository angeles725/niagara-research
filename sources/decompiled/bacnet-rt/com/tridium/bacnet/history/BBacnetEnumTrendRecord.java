package com.tridium.bacnet.history;

import com.tridium.bacnet.datatypes.BTrendEvent;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import javax.baja.history.BTrendFlags;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.status.BStatus;
import javax.baja.status.BStatusEnum;
import javax.baja.status.BStatusValue;
import javax.baja.sys.BAbsTime;
import javax.baja.sys.BDynamicEnum;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperty(
   name = "value",
   type = "BDynamicEnum",
   defaultValue = "BDynamicEnum.DEFAULT",
   flags = 8
)
public class BBacnetEnumTrendRecord extends BBacnetTrendRecord {
   public static final Property value = newProperty(8, BDynamicEnum.DEFAULT, null);
   public static final Type TYPE = Sys.loadType(BBacnetEnumTrendRecord.class);

   public BDynamicEnum getValue() {
      return (BDynamicEnum)this.get(value);
   }

   public void setValue(BDynamicEnum v) {
      this.set(value, v, null);
   }

   @Override
   public Type getType() {
      return TYPE;
   }

   public BBacnetEnumTrendRecord(BAbsTime timestamp, BDynamicEnum value, BStatus status, long seqNum) {
      super(timestamp, status, seqNum);
      this.setValue(value);
   }

   public BBacnetEnumTrendRecord(BAbsTime timestamp, BDynamicEnum value, BStatus status, long seqNum, BTrendEvent event) {
      super(timestamp, status, seqNum, event);
      this.setValue(value);
   }

   public BBacnetEnumTrendRecord() {
   }

   @Override
   public int getLogDatumType() {
      int eventCheck = super.getLogDatumType();
      if (eventCheck != -1) {
         return eventCheck;
      } else {
         return this.getStatus().isNull() ? 7 : 3;
      }
   }

   public Property getValueProperty() {
      return value;
   }

   public boolean isFixedSize() {
      return true;
   }

   @Override
   public BBacnetTrendRecord set(BAbsTime timestamp, BStatus status, long seqNum, BTrendEvent event, BTrendFlags trendFlags) {
      this.setTimestamp(timestamp);
      this.setValue(BDynamicEnum.DEFAULT);
      this.setStatus(status);
      this.setSequenceNumber(seqNum);
      this.setLogEvent(event);
      this.setTrendFlags(trendFlags);
      return this;
   }

   @Override
   public BBacnetTrendRecord set(BAbsTime timestamp, BStatusValue out, long seqNum, BTrendEvent event, BTrendFlags trendFlags) {
      this.setTimestamp(timestamp);
      this.setValue(((BStatusEnum)out).getValue());
      this.setStatus(out.getStatus());
      this.setSequenceNumber(seqNum);
      this.setLogEvent(event);
      this.setTrendFlags(trendFlags);
      return this;
   }

   @Override
   protected void doReadTrend(DataInput in) throws IOException {
      super.doReadTrend(in);
      this.setValue(BDynamicEnum.make(in.readInt()));
   }

   @Override
   protected void doWriteTrend(DataOutput out) throws IOException {
      super.doWriteTrend(out);
      out.writeInt(this.getValue().getOrdinal());
   }

   public String toString(Context ctx) {
      StringBuilder s = new StringBuilder(32);
      s.append(super.toString(ctx));
      s.append(" ");
      s.append(this.getValue().toString(ctx));
      s.append(' ');
      s.append(this.getStatus());
      return s.toString();
   }
}
