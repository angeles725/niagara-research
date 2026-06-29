package com.tridium.bacnet.history;

import com.tridium.bacnet.datatypes.BTrendEvent;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import javax.baja.history.BTrendFlags;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.status.BStatus;
import javax.baja.status.BStatusNumeric;
import javax.baja.status.BStatusValue;
import javax.baja.sys.BAbsTime;
import javax.baja.sys.BFacets;
import javax.baja.sys.BINumeric;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperty(
   name = "value",
   type = "double",
   defaultValue = "0d",
   flags = 8
)
public class BBacnetNumericTrendRecord extends BBacnetTrendRecord implements BINumeric {
   public static final Property value = newProperty(8, 0.0, null);
   public static final Type TYPE = Sys.loadType(BBacnetNumericTrendRecord.class);

   public double getValue() {
      return this.getDouble(value);
   }

   public void setValue(double v) {
      this.setDouble(value, v, null);
   }

   @Override
   public Type getType() {
      return TYPE;
   }

   public BBacnetNumericTrendRecord(BAbsTime timestamp, double value, BStatus status, long seqNum) {
      super(timestamp, status, seqNum);
      this.setValue(value);
   }

   public BBacnetNumericTrendRecord(BAbsTime timestamp, double value, BStatus status, long seqNum, BTrendEvent event) {
      super(timestamp, status, seqNum, event);
      this.setValue(value);
   }

   public BBacnetNumericTrendRecord() {
   }

   @Override
   public int getLogDatumType() {
      int eventCheck = super.getLogDatumType();
      if (eventCheck != -1) {
         return eventCheck;
      } else {
         return this.getStatus().isNull() ? 7 : 2;
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
      this.setValue(0.0);
      this.setStatus(status);
      this.setSequenceNumber(seqNum);
      this.setLogEvent(event);
      this.setTrendFlags(trendFlags);
      return this;
   }

   @Override
   public BBacnetTrendRecord set(BAbsTime timestamp, BStatusValue out, long seqNum, BTrendEvent event, BTrendFlags trendFlags) {
      this.setTimestamp(timestamp);
      this.setValue(((BStatusNumeric)out).getValue());
      this.setStatus(out.getStatus());
      this.setSequenceNumber(seqNum);
      this.setLogEvent(event);
      this.setTrendFlags(trendFlags);
      return this;
   }

   @Override
   protected void doReadTrend(DataInput in) throws IOException {
      super.doReadTrend(in);
      this.setValue(in.readFloat());
   }

   @Override
   protected void doWriteTrend(DataOutput out) throws IOException {
      super.doWriteTrend(out);
      out.writeFloat((float)this.getValue());
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

   public double getNumeric() {
      return this.getValue();
   }

   public BFacets getNumericFacets() {
      return BFacets.NULL;
   }
}
