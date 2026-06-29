package com.tridium.bacnet.history;

import com.tridium.bacnet.datatypes.BTrendEvent;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import javax.baja.history.BTrendFlags;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.status.BStatus;
import javax.baja.status.BStatusBoolean;
import javax.baja.status.BStatusValue;
import javax.baja.sys.BAbsTime;
import javax.baja.sys.BBoolean;
import javax.baja.sys.BEnum;
import javax.baja.sys.BFacets;
import javax.baja.sys.BIBoolean;
import javax.baja.sys.BIEnum;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperty(
   name = "value",
   type = "boolean",
   defaultValue = "false",
   flags = 8
)
public class BBacnetBooleanTrendRecord extends BBacnetTrendRecord implements BIEnum, BIBoolean {
   public static final Property value = newProperty(8, false, null);
   public static final Type TYPE = Sys.loadType(BBacnetBooleanTrendRecord.class);

   public boolean getValue() {
      return this.getBoolean(value);
   }

   public void setValue(boolean v) {
      this.setBoolean(value, v, null);
   }

   @Override
   public Type getType() {
      return TYPE;
   }

   public BBacnetBooleanTrendRecord(BAbsTime timestamp, boolean value, BStatus status, long seqNum) {
      super(timestamp, status, seqNum);
      this.setValue(value);
   }

   public BBacnetBooleanTrendRecord(BAbsTime timestamp, boolean value, BStatus status, long seqNum, BTrendEvent event) {
      super(timestamp, status, seqNum, event);
      this.setValue(value);
   }

   public BBacnetBooleanTrendRecord() {
   }

   @Override
   public int getLogDatumType() {
      int eventCheck = super.getLogDatumType();
      if (eventCheck != -1) {
         return eventCheck;
      } else {
         return this.getStatus().isNull() ? 7 : 1;
      }
   }

   public Property getValueProperty() {
      return value;
   }

   public BEnum getEnum() {
      return BBoolean.make(this.getValue());
   }

   public BFacets getEnumFacets() {
      return BFacets.NULL;
   }

   public boolean getBoolean() {
      return this.getValue();
   }

   public BFacets getBooleanFacets() {
      return BFacets.NULL;
   }

   public boolean isFixedSize() {
      return true;
   }

   @Override
   public BBacnetTrendRecord set(BAbsTime timestamp, BStatus status, long seqNum, BTrendEvent event, BTrendFlags trendFlags) {
      this.setTimestamp(timestamp);
      this.setValue(false);
      this.setStatus(status);
      this.setSequenceNumber(seqNum);
      this.setLogEvent(event);
      this.setTrendFlags(trendFlags);
      return this;
   }

   @Override
   public BBacnetTrendRecord set(BAbsTime timestamp, BStatusValue out, long seqNum, BTrendEvent event, BTrendFlags trendFlags) {
      this.setTimestamp(timestamp);
      this.setValue(((BStatusBoolean)out).getValue());
      this.setStatus(out.getStatus());
      this.setSequenceNumber(seqNum);
      this.setLogEvent(event);
      this.setTrendFlags(trendFlags);
      return this;
   }

   @Override
   public void doReadTrend(DataInput in) throws IOException {
      super.doReadTrend(in);
      this.setValue(in.readBoolean());
   }

   @Override
   public void doWriteTrend(DataOutput out) throws IOException {
      super.doWriteTrend(out);
      out.writeBoolean(this.getValue());
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
