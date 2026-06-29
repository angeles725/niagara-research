package javax.baja.bacnet.datatypes;

import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.io.AsnInput;
import javax.baja.bacnet.io.AsnOutput;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BComponent;
import javax.baja.sys.BSimple;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "time",
      type = "BBacnetTime",
      defaultValue = "BBacnetTime.DEFAULT"
   ), @NiagaraProperty(
      name = "value",
      type = "BBacnetAny",
      defaultValue = "new BBacnetAny()"
   )})
public final class BBacnetTimeValue extends BComponent implements BIBacnetDataType, Comparable<Object> {
   public static final Property time = newProperty(0, BBacnetTime.DEFAULT, null);
   public static final Property value = newProperty(0, new BBacnetAny(), null);
   public static final Type TYPE = Sys.loadType(BBacnetTimeValue.class);

   public BBacnetTime getTime() {
      return (BBacnetTime)this.get(time);
   }

   public void setTime(BBacnetTime v) {
      this.set(time, v, null);
   }

   public BBacnetAny getValue() {
      return (BBacnetAny)this.get(value);
   }

   public void setValue(BBacnetAny v) {
      this.set(value, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BBacnetTimeValue() {
   }

   public BBacnetTimeValue(BBacnetTime time, BSimple value) {
      this.setTime(time);
      this.getValue().setAny(value, null);
   }

   public void changed(Property p, Context cx) {
      super.changed(p, cx);
      if (this.isMounted() && this.isRunning()) {
         this.getParent().asComponent().changed(this.getPropertyInParent(), cx);
      }
   }

   @Override
   public void writeAsn(AsnOutput out) {
      out.writeTime(this.getTime());
      this.getValue().writeAsn(out);
   }

   @Override
   public void readAsn(AsnInput in) throws AsnException {
      BBacnetTime time = in.readTime();
      this.getValue().readAsn(in);
      this.set(BBacnetTimeValue.time, time, noWrite);
   }

   @Override
   public int compareTo(Object obj) {
      BBacnetTime tOther = ((BBacnetTimeValue)obj).getTime();
      BBacnetTime tThis = this.getTime();
      return tThis.compareTo(tOther);
   }

   public boolean isBefore(BBacnetTimeValue x) {
      return this.compareTo(x) < 0;
   }

   public boolean isAfter(BBacnetTimeValue x) {
      return this.compareTo(x) > 0;
   }

   public boolean isNotBefore(BBacnetTimeValue x) {
      return this.compareTo(x) >= 0;
   }

   public boolean isNotAfter(BBacnetTimeValue x) {
      return this.compareTo(x) <= 0;
   }

   public String toString(Context context) {
      return context != null && context.equals(nameContext)
         ? this.getTime().toString(context) + "_" + this.getValue().toString(context)
         : this.getTime().toString(context) + ";" + this.getValue().toString(context);
   }
}
