package com.tridium.bacnet.history;

import com.tridium.bacnet.datatypes.BTrendEvent;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import javax.baja.history.BTrendFlags;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.status.BStatus;
import javax.baja.status.BStatusString;
import javax.baja.status.BStatusValue;
import javax.baja.sys.BAbsTime;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperty(
   name = "value",
   type = "String",
   defaultValue = "",
   flags = 8
)
public class BBacnetStringTrendRecord extends BBacnetTrendRecord {
   public static final Property value = newProperty(8, "", null);
   public static final Type TYPE = Sys.loadType(BBacnetStringTrendRecord.class);

   public String getValue() {
      return this.getString(value);
   }

   public void setValue(String v) {
      this.setString(value, v, null);
   }

   @Override
   public Type getType() {
      return TYPE;
   }

   public BBacnetStringTrendRecord() {
   }

   public BBacnetStringTrendRecord(BAbsTime timestamp, String s, BStatus status, long seqNum) {
      super(timestamp, status, seqNum);
      this.setValue(s);
   }

   public BBacnetStringTrendRecord(BAbsTime timestamp, String s, BStatus status, long seqNum, BTrendEvent event) {
      super(timestamp, status, seqNum, event);
      this.setValue(s);
   }

   @Override
   public int getLogDatumType() {
      int eventCheck = super.getLogDatumType();
      if (eventCheck != -1) {
         return eventCheck;
      } else {
         return this.getStatus().isNull() ? 7 : 10;
      }
   }

   public Property getValueProperty() {
      return value;
   }

   public boolean isFixedSize() {
      return false;
   }

   @Override
   protected void doReadTrend(DataInput in) throws IOException {
      super.doReadTrend(in);
      this.setValue(in.readUTF());
   }

   @Override
   protected void doWriteTrend(DataOutput out) throws IOException {
      super.doWriteTrend(out);
      out.writeUTF(this.getValue());
   }

   @Override
   public BBacnetTrendRecord set(BAbsTime timestamp, BStatus status, long seqNum, BTrendEvent event, BTrendFlags trendFlags) {
      this.setTimestamp(timestamp);
      this.setValue("");
      this.setStatus(status);
      this.setSequenceNumber(seqNum);
      this.setLogEvent(event);
      this.setTrendFlags(trendFlags);
      return this;
   }

   @Override
   public BBacnetTrendRecord set(BAbsTime timestamp, BStatusValue out, long seqNum, BTrendEvent event, BTrendFlags trendFlags) {
      this.setTimestamp(timestamp);
      this.setValue(((BStatusString)out).getValue());
      this.setStatus(out.getStatus());
      this.setSequenceNumber(seqNum);
      this.setLogEvent(event);
      this.setTrendFlags(trendFlags);
      return this;
   }

   public String toString(Context ctx) {
      StringBuilder s = new StringBuilder(32);
      s.append(super.toString(ctx));
      s.append(" ");
      s.append(this.getValue());
      s.append(' ');
      s.append(this.getStatus());
      return s.toString();
   }
}
