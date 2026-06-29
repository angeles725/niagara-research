package javax.baja.bacnet.datatypes;

import javax.baja.bacnet.enums.BBacnetAccumulatorStatus;
import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.io.AsnInput;
import javax.baja.bacnet.io.AsnOutput;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BStruct;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "timestamp",
      type = "BBacnetDateTime",
      defaultValue = "new BBacnetDateTime()"
   ), @NiagaraProperty(
      name = "presentValue",
      type = "BBacnetUnsigned",
      defaultValue = "BBacnetUnsigned.DEFAULT"
   ), @NiagaraProperty(
      name = "accumulateValue",
      type = "BBacnetUnsigned",
      defaultValue = "BBacnetUnsigned.DEFAULT"
   ), @NiagaraProperty(
      name = "accumulatorStatus",
      type = "BBacnetAccumulatorStatus",
      defaultValue = "BBacnetAccumulatorStatus.normal"
   )})
public final class BBacnetAccumulatorRecord extends BStruct implements BIBacnetDataType {
   public static final Property timestamp = newProperty(0, new BBacnetDateTime(), null);
   public static final Property presentValue = newProperty(0, BBacnetUnsigned.DEFAULT, null);
   public static final Property accumulateValue = newProperty(0, BBacnetUnsigned.DEFAULT, null);
   public static final Property accumulatorStatus = newProperty(0, BBacnetAccumulatorStatus.normal, null);
   public static final Type TYPE = Sys.loadType(BBacnetAccumulatorRecord.class);

   public BBacnetDateTime getTimestamp() {
      return (BBacnetDateTime)this.get(timestamp);
   }

   public void setTimestamp(BBacnetDateTime v) {
      this.set(timestamp, v, null);
   }

   public BBacnetUnsigned getPresentValue() {
      return (BBacnetUnsigned)this.get(presentValue);
   }

   public void setPresentValue(BBacnetUnsigned v) {
      this.set(presentValue, v, null);
   }

   public BBacnetUnsigned getAccumulateValue() {
      return (BBacnetUnsigned)this.get(accumulateValue);
   }

   public void setAccumulateValue(BBacnetUnsigned v) {
      this.set(accumulateValue, v, null);
   }

   public BBacnetAccumulatorStatus getAccumulatorStatus() {
      return (BBacnetAccumulatorStatus)this.get(accumulatorStatus);
   }

   public void setAccumulatorStatus(BBacnetAccumulatorStatus v) {
      this.set(accumulatorStatus, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   @Override
   public void writeAsn(AsnOutput out) {
      out.writeOpeningTag(0);
      this.getTimestamp().writeAsn(out);
      out.writeClosingTag(0);
      out.writeUnsigned(1, this.getPresentValue());
      out.writeUnsigned(2, this.getAccumulateValue());
      out.writeEnumerated(3, this.getAccumulatorStatus().getOrdinal());
   }

   @Override
   public void readAsn(AsnInput in) throws AsnException {
      in.skipOpeningTag(0);
      BBacnetDate date = in.readDate();
      BBacnetTime time = in.readTime();
      in.skipClosingTag(0);
      BBacnetUnsigned presentValue = in.readUnsigned(1);
      BBacnetUnsigned accumulateValue = in.readUnsigned(2);
      int accumulatorStatus = in.readEnumerated(3);
      BBacnetDateTime timestamp = this.getTimestamp();
      timestamp.set(BBacnetDateTime.date, date, noWrite);
      timestamp.set(BBacnetDateTime.time, time, noWrite);
      this.set(BBacnetAccumulatorRecord.presentValue, presentValue, noWrite);
      this.set(BBacnetAccumulatorRecord.accumulateValue, accumulateValue, noWrite);
      this.set(BBacnetAccumulatorRecord.accumulatorStatus, BBacnetAccumulatorStatus.make(accumulatorStatus), noWrite);
   }

   public String toString(Context context) {
      return "BBacnetAccumulatorRecord: timestamp = "
         + this.getTimestamp().toString(context)
         + "; presentValue = "
         + this.getPresentValue().toString(context)
         + "; accumulateValue = "
         + this.getAccumulateValue().toString(context)
         + "; accumulatorStatus = "
         + this.getAccumulatorStatus().toString(context);
   }
}
