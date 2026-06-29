package javax.baja.lonworks.londata;

import javax.baja.lonworks.enums.BLonDaysOfWeekEnum;
import javax.baja.lonworks.enums.BLonElementType;
import javax.baja.lonworks.enums.BLonIntervalOfMonthEnum;
import javax.baja.lonworks.io.LonInputStream;
import javax.baja.lonworks.io.LonOutputStream;
import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "units",
      type = "BLonEnum",
      defaultValue = "BLonEnum.make(BLonIntervalOfMonthEnum.DEFAULT)",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.e8, null)")}
   ), @NiagaraProperty(
      name = "valueMinutesInterval",
      type = "BLonFloat",
      defaultValue = "BLonFloat  .DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.ub, 1.0F, null)")}
   ), @NiagaraProperty(
      name = "valueDateOfMonth",
      type = "BLonFloat",
      defaultValue = "BLonFloat  .DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.ub, 1.0F, 31.0F, 1.0F, null)")}
   ), @NiagaraProperty(
      name = "valueHourOfDay",
      type = "BLonFloat",
      defaultValue = "BLonFloat  .DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.ub, 0.0F, 23.0F, 1.0F, null)")}
   ), @NiagaraProperty(
      name = "valueDayOfWeek",
      type = "BLonEnum",
      defaultValue = "BLonEnum.make(BLonDaysOfWeekEnum.DEFAULT)",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.e8, null)")}
   ), @NiagaraProperty(
      name = "valueHoursInterval",
      type = "BLonFloat",
      defaultValue = "BLonFloat  .DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.ub, 1.0F, null)")}
   )})
public class BLonCpTimePeriod extends BLonData {
   public static final Property units = newProperty(0, BLonEnum.make(BLonIntervalOfMonthEnum.DEFAULT), LonFacetsUtil.makeFacets(BLonElementType.e8, null));
   public static final Property valueMinutesInterval = newProperty(0, BLonFloat.DEFAULT, LonFacetsUtil.makeFacets(BLonElementType.ub, 1.0F, null));
   public static final Property valueDateOfMonth = newProperty(0, BLonFloat.DEFAULT, LonFacetsUtil.makeFacets(BLonElementType.ub, 1.0F, 31.0F, 1.0F, null));
   public static final Property valueHourOfDay = newProperty(0, BLonFloat.DEFAULT, LonFacetsUtil.makeFacets(BLonElementType.ub, 0.0F, 23.0F, 1.0F, null));
   public static final Property valueDayOfWeek = newProperty(0, BLonEnum.make(BLonDaysOfWeekEnum.DEFAULT), LonFacetsUtil.makeFacets(BLonElementType.e8, null));
   public static final Property valueHoursInterval = newProperty(0, BLonFloat.DEFAULT, LonFacetsUtil.makeFacets(BLonElementType.ub, 1.0F, null));
   public static final Type TYPE = Sys.loadType(BLonCpTimePeriod.class);

   public BLonEnum getUnits() {
      return (BLonEnum)this.get(units);
   }

   public void setUnits(BLonEnum v) {
      this.set(units, v, null);
   }

   public BLonFloat getValueMinutesInterval() {
      return (BLonFloat)this.get(valueMinutesInterval);
   }

   public void setValueMinutesInterval(BLonFloat v) {
      this.set(valueMinutesInterval, v, null);
   }

   public BLonFloat getValueDateOfMonth() {
      return (BLonFloat)this.get(valueDateOfMonth);
   }

   public void setValueDateOfMonth(BLonFloat v) {
      this.set(valueDateOfMonth, v, null);
   }

   public BLonFloat getValueHourOfDay() {
      return (BLonFloat)this.get(valueHourOfDay);
   }

   public void setValueHourOfDay(BLonFloat v) {
      this.set(valueHourOfDay, v, null);
   }

   public BLonEnum getValueDayOfWeek() {
      return (BLonEnum)this.get(valueDayOfWeek);
   }

   public void setValueDayOfWeek(BLonEnum v) {
      this.set(valueDayOfWeek, v, null);
   }

   public BLonFloat getValueHoursInterval() {
      return (BLonFloat)this.get(valueHoursInterval);
   }

   public void setValueHoursInterval(BLonFloat v) {
      this.set(valueHoursInterval, v, null);
   }

   @Override
   public Type getType() {
      return TYPE;
   }

   @Override
   public void toOutputStream(LonOutputStream out) {
      this.primitiveToOutputStream(units, out);
      switch (this.getUnits().getEnum().getOrdinal()) {
         case 0:
            this.primitiveToOutputStream(valueMinutesInterval, out);
            break;
         case 1:
            this.primitiveToOutputStream(valueDateOfMonth, out);
            break;
         case 2:
            this.primitiveToOutputStream(valueHourOfDay, out);
            break;
         case 3:
            this.primitiveToOutputStream(valueDayOfWeek, out);
            break;
         case 4:
            this.primitiveToOutputStream(valueHoursInterval, out);
            break;
         default:
            out.writeUnsigned8(0);
      }
   }

   @Override
   public void fromInputStream(LonInputStream in) {
      this.primitiveFromInputStream(units, in);
      switch (this.getUnits().getEnum().getOrdinal()) {
         case 0:
            this.primitiveFromInputStream(valueMinutesInterval, in);
            break;
         case 1:
            this.primitiveFromInputStream(valueDateOfMonth, in);
            break;
         case 2:
            this.primitiveFromInputStream(valueHourOfDay, in);
            break;
         case 3:
            this.primitiveFromInputStream(valueDayOfWeek, in);
            break;
         case 4:
            this.primitiveFromInputStream(valueHoursInterval, in);
      }
   }
}
