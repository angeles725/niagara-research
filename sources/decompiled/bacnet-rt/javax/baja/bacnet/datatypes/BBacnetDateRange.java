package javax.baja.bacnet.datatypes;

import javax.baja.bacnet.BacnetConst;
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
      name = "startDate",
      type = "BBacnetDate",
      defaultValue = "BBacnetDate.DEFAULT"
   ), @NiagaraProperty(
      name = "endDate",
      type = "BBacnetDate",
      defaultValue = "BBacnetDate.DEFAULT"
   )})
public class BBacnetDateRange extends BStruct implements BIBacnetDataType {
   public static final Property startDate = newProperty(0, BBacnetDate.DEFAULT, null);
   public static final Property endDate = newProperty(0, BBacnetDate.DEFAULT, null);
   public static final Type TYPE = Sys.loadType(BBacnetDateRange.class);

   public BBacnetDate getStartDate() {
      return (BBacnetDate)this.get(startDate);
   }

   public void setStartDate(BBacnetDate v) {
      this.set(startDate, v, null);
   }

   public BBacnetDate getEndDate() {
      return (BBacnetDate)this.get(endDate);
   }

   public void setEndDate(BBacnetDate v) {
      this.set(endDate, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BBacnetDateRange() {
   }

   public BBacnetDateRange(BBacnetDate startDate, BBacnetDate endDate) {
      this.setStartDate(startDate);
      this.setEndDate(endDate);
   }

   @Override
   public final void writeAsn(AsnOutput out) {
      out.writeDate(this.getStartDate());
      out.writeDate(this.getEndDate());
   }

   @Override
   public final void readAsn(AsnInput in) throws AsnException {
      BBacnetDate startDate = in.readDate();
      BBacnetDate endDate = in.readDate();
      this.set(BBacnetDateRange.startDate, startDate, noWrite);
      this.set(BBacnetDateRange.endDate, endDate, noWrite);
   }

   public String toString(Context context) {
      char sep = ';';
      if (context != null && context.equals(BacnetConst.facetsContext)) {
         sep = '_';
      }

      return this.getStartDate().toString(context) + sep + this.getEndDate().toString(context);
   }

   public static BBacnetDateRange fromString(String s) {
      BBacnetDate sd = BBacnetDate.fromString(s.substring(0, 14));
      BBacnetDate ed = BBacnetDate.fromString(s.substring(15, 29));
      return new BBacnetDateRange(sd, ed);
   }
}
