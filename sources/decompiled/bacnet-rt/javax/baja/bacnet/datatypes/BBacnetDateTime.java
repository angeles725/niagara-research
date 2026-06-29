package javax.baja.bacnet.datatypes;

import javax.baja.bacnet.BacnetConst;
import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.io.AsnInput;
import javax.baja.bacnet.io.AsnOutput;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BAbsTime;
import javax.baja.sys.BMonth;
import javax.baja.sys.BStruct;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "date",
      type = "BBacnetDate",
      defaultValue = "BBacnetDate.DEFAULT"
   ), @NiagaraProperty(
      name = "time",
      type = "BBacnetTime",
      defaultValue = "BBacnetTime.DEFAULT"
   )})
public class BBacnetDateTime extends BStruct implements BIBacnetDataType, Comparable<Object> {
   public static final Property date = newProperty(0, BBacnetDate.DEFAULT, null);
   public static final Property time = newProperty(0, BBacnetTime.DEFAULT, null);
   public static final Type TYPE = Sys.loadType(BBacnetDateTime.class);

   public BBacnetDate getDate() {
      return (BBacnetDate)this.get(date);
   }

   public void setDate(BBacnetDate v) {
      this.set(date, v, null);
   }

   public BBacnetTime getTime() {
      return (BBacnetTime)this.get(time);
   }

   public void setTime(BBacnetTime v) {
      this.set(time, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BBacnetDateTime() {
   }

   public BBacnetDateTime(BBacnetDate date, BBacnetTime time) {
      this.setDate(date);
      this.setTime(time);
   }

   public BBacnetDateTime(BAbsTime bt) {
      this.setDate(BBacnetDate.make(bt));
      this.setTime(BBacnetTime.make(bt));
   }

   @Override
   public final void writeAsn(AsnOutput out) {
      out.writeDate(this.getDate());
      out.writeTime(this.getTime());
   }

   @Override
   public final void readAsn(AsnInput in) throws AsnException {
      BBacnetDate date = in.readDate();
      BBacnetTime time = in.readTime();
      this.set(BBacnetDateTime.date, date, noWrite);
      this.set(BBacnetDateTime.time, time, noWrite);
   }

   public String toString(Context context) {
      return context != null && context.equals(BacnetConst.facetsContext)
         ? this.getDate().toString(context, false) + ' ' + this.getTime().toString(context)
         : this.getDate().toString(context) + '_' + this.getTime().toString(context);
   }

   public final boolean isAnyUnspecified() {
      return this.getDate().isAnyUnspecified() || this.getTime().isAnyUnspecified();
   }

   public final BAbsTime toBAbsTime() {
      return makeBAbsTime(this.getDate(), this.getTime());
   }

   public final void fromBAbsTime(BAbsTime t) {
      this.setDate(BBacnetDate.make(t));
      this.setTime(BBacnetTime.make(t));
   }

   public final boolean dateTimeEquals(Object obj) {
      return this.compareTo(obj) == 0;
   }

   @Override
   public final int compareTo(Object obj) {
      if (obj == null) {
         throw new ClassCastException();
      } else {
         BBacnetDateTime other = (BBacnetDateTime)obj;
         int ret = this.getDate().compareTo(other.getDate());
         return ret != 0 ? ret : this.getTime().compareTo(other.getTime());
      }
   }

   public final boolean isBefore(Object x) {
      return this.compareTo(x) < 0;
   }

   public final boolean isAfter(Object x) {
      return this.compareTo(x) > 0;
   }

   public final boolean isNotBefore(Object x) {
      return this.compareTo(x) >= 0;
   }

   public final boolean isNotAfter(Object x) {
      return this.compareTo(x) <= 0;
   }

   public static final BBacnetDateTime fromString(String s) {
      BBacnetDate d = BBacnetDate.fromString(s.substring(0, 14));
      BBacnetTime t = BBacnetTime.fromString(s.substring(15, 26));
      return new BBacnetDateTime(d, t);
   }

   public static final BAbsTime makeBAbsTime(BBacnetDate d, BBacnetTime t) {
      int y = d.isYearUnspecified() ? 1900 : d.getYear();
      BMonth m = d.isMonthUnspecified() ? BMonth.january : d.getBMonth();
      int a = d.isDayOfMonthUnspecified() ? 1 : d.getDayOfMonth();
      int h = t.isHourUnspecified() ? 0 : t.getHour();
      int n = t.isMinuteUnspecified() ? 0 : t.getMinute();
      return !t.isSecondUnspecified() && !t.isHundredthUnspecified()
         ? BAbsTime.make(y, m, a, h, n, t.getSecond(), t.getHundredth() * 10)
         : BAbsTime.make(y, m, a, h, n);
   }

   public static final BAbsTime makeBAbsTime(BAbsTime d, BBacnetTime t) {
      int h = t.isHourUnspecified() ? 0 : t.getHour();
      int n = t.isMinuteUnspecified() ? 0 : t.getMinute();
      return !t.isSecondUnspecified() && !t.isHundredthUnspecified()
         ? BAbsTime.make(d.getYear(), d.getMonth(), d.getDay(), t.getHour(), t.getMinute(), t.getSecond(), t.getHundredth() * 10)
         : BAbsTime.make(d.getYear(), d.getMonth(), d.getDay(), h, n);
   }
}
