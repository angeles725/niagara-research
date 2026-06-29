package com.tridium.bacnet.history;

import com.tridium.bacnet.datatypes.BTrendEvent;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import javax.baja.history.BTrendFlags;
import javax.baja.history.BTrendRecord;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.status.BIStatus;
import javax.baja.status.BStatus;
import javax.baja.status.BStatusValue;
import javax.baja.sys.BAbsTime;
import javax.baja.sys.BLong;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "sequenceNumber",
      type = "long",
      defaultValue = "BLong.make(-1)"
   ), @NiagaraProperty(
      name = "logEvent",
      type = "BTrendEvent",
      defaultValue = "BTrendEvent.DEFAULT"
   )})
public abstract class BBacnetTrendRecord extends BTrendRecord implements BIStatus {
   public static final Property sequenceNumber = newProperty(0, BLong.make(-1L), null);
   public static final Property logEvent = newProperty(0, BTrendEvent.DEFAULT, null);
   public static final Type TYPE = Sys.loadType(BBacnetTrendRecord.class);

   public long getSequenceNumber() {
      return this.getLong(sequenceNumber);
   }

   public void setSequenceNumber(long v) {
      this.setLong(sequenceNumber, v, null);
   }

   public BTrendEvent getLogEvent() {
      return (BTrendEvent)this.get(logEvent);
   }

   public void setLogEvent(BTrendEvent v) {
      this.set(logEvent, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BBacnetTrendRecord() {
   }

   public BBacnetTrendRecord(BAbsTime timestamp, BStatus status, long seqNum) {
      super(timestamp, status);
      this.setSequenceNumber(seqNum);
   }

   public BBacnetTrendRecord(BAbsTime timestamp, BStatus status, long seqNum, BTrendEvent event) {
      super(timestamp, status);
      this.setSequenceNumber(seqNum);
      this.setLogEvent(event);
   }

   public int getLogDatumType() {
      BTrendEvent tEvent = this.getLogEvent();
      if (tEvent.isLogStatus()) {
         return 0;
      } else if (tEvent.isFailure()) {
         return 8;
      } else {
         return tEvent.isTimeChange() ? 9 : -1;
      }
   }

   protected void doReadTrend(DataInput in) throws IOException {
      this.setSequenceNumber(in.readLong());
      this.setLogEvent((BTrendEvent)this.getLogEvent().decode(in));
   }

   protected void doWriteTrend(DataOutput out) throws IOException {
      out.writeLong(this.getSequenceNumber());
      this.getLogEvent().encode(out);
   }

   public abstract BBacnetTrendRecord set(BAbsTime var1, BStatus var2, long var3, BTrendEvent var5, BTrendFlags var6);

   public abstract BBacnetTrendRecord set(BAbsTime var1, BStatusValue var2, long var3, BTrendEvent var5, BTrendFlags var6);
}
