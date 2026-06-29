package javax.baja.lonworks.londata;

import javax.baja.lonworks.enums.BLonCalendarTypeEnum;
import javax.baja.lonworks.enums.BLonDaysOfWeekEnum;
import javax.baja.lonworks.enums.BLonElementType;
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
      name = "secondTimeOffset",
      type = "BLonFloat",
      defaultValue = "BLonFloat.DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.s32, -86400F, 86400F, 1, null)")}
   ), @NiagaraProperty(
      name = "typeOfDescription",
      type = "BLonEnum",
      defaultValue = "BLonEnum.make(BLonCalendarTypeEnum.calNul)",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.e8, null)")}
   ), @NiagaraProperty(
      name = "hourOfStartDST",
      type = "BLonFloat",
      defaultValue = "BLonFloat.DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.u8, 0, 23, 1, 5, null)")}
   ), @NiagaraProperty(
      name = "minuteOfStartDST",
      type = "BLonFloat",
      defaultValue = "BLonFloat.DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.u8, 0, 59, 1, 6, null)")}
   ), @NiagaraProperty(
      name = "secondOfStartDST",
      type = "BLonFloat",
      defaultValue = "BLonFloat.DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.u8, 0, 59, 1, 7, null)")}
   ), @NiagaraProperty(
      name = "GdayOfStartDST",
      type = "BLonFloat",
      defaultValue = "BLonFloat.DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.u16,true,0F, true,365F, 1F,0F, true,8,0, false,0F, 1, null)")}
   ), @NiagaraProperty(
      name = "JdayOfStartDST",
      type = "BLonFloat",
      defaultValue = "BLonFloat.DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.u16,true,1F, true,365F, 1F,0F, true,8,0, false,0F, 1, null)")}
   ), @NiagaraProperty(
      name = "MmonthOfStartDST",
      type = "BLonFloat",
      defaultValue = "BLonFloat.DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.ub,true,1F, true,12F, 1F,0F, true,8,4, false,0F, 4, null)")}
   ), @NiagaraProperty(
      name = "MweekOfStartDST",
      type = "BLonFloat",
      defaultValue = "BLonFloat.DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.ub,true,1F, true,5F, 1F,0F, true,8,1, false,0F, 3, null)")}
   ), @NiagaraProperty(
      name = "MdatedayOfStartDST",
      type = "BLonEnum",
      defaultValue = "BLonEnum.make(BLonDaysOfWeekEnum.daySun)",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.e8, 9)")}
   ), @NiagaraProperty(
      name = "hourOfEndDST",
      type = "BLonFloat",
      defaultValue = "BLonFloat.DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.u8, 0, 23, 1, 10, null)")}
   ), @NiagaraProperty(
      name = "minuteOfEndDST",
      type = "BLonFloat",
      defaultValue = "BLonFloat.DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.u8, 0, 59, 1, 11, null)")}
   ), @NiagaraProperty(
      name = "secondOfEndDST",
      type = "BLonFloat",
      defaultValue = "BLonFloat.DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.u8, 0, 59, 1, 12, null)")}
   ), @NiagaraProperty(
      name = "GdayOfEndDST",
      type = "BLonFloat",
      defaultValue = "BLonFloat.DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.u16,true,0F, true,365F, 1F,0F, true,13,0, false,0F, 1, null)")}
   ), @NiagaraProperty(
      name = "JdayOfEndDST",
      type = "BLonFloat",
      defaultValue = "BLonFloat.DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.u16,true,1F, true,365F, 1F,0F, true,13,0, false,0F, 1, null)")}
   ), @NiagaraProperty(
      name = "MmonthOfEndDST",
      type = "BLonFloat",
      defaultValue = "BLonFloat.DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.ub,true,1F, true,12F, 1F,0F, true,13,4, false,0F, 4, null)")}
   ), @NiagaraProperty(
      name = "MweekOfEndDST",
      type = "BLonFloat",
      defaultValue = "BLonFloat.DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.ub,true,1F, true,5F, 1F,0F, true,13,1, false,0F, 3, null)")}
   ), @NiagaraProperty(
      name = "MgatedayOfEndDST",
      type = "BLonEnum",
      defaultValue = "BLonEnum.make(BLonDaysOfWeekEnum.daySun)",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.e8, 14)")}
   )})
public class BLonTimeZone extends BLonData {
   public static final Property secondTimeOffset = newProperty(
      0, BLonFloat.DEFAULT, LonFacetsUtil.makeFacets(BLonElementType.s32, -86400.0F, 86400.0F, 1.0F, null)
   );
   public static final Property typeOfDescription = newProperty(
      0, BLonEnum.make(BLonCalendarTypeEnum.calNul), LonFacetsUtil.makeFacets(BLonElementType.e8, null)
   );
   public static final Property hourOfStartDST = newProperty(0, BLonFloat.DEFAULT, LonFacetsUtil.makeFacets(BLonElementType.u8, 0.0F, 23.0F, 1.0F, 5, null));
   public static final Property minuteOfStartDST = newProperty(0, BLonFloat.DEFAULT, LonFacetsUtil.makeFacets(BLonElementType.u8, 0.0F, 59.0F, 1.0F, 6, null));
   public static final Property secondOfStartDST = newProperty(0, BLonFloat.DEFAULT, LonFacetsUtil.makeFacets(BLonElementType.u8, 0.0F, 59.0F, 1.0F, 7, null));
   public static final Property GdayOfStartDST = newProperty(
      0, BLonFloat.DEFAULT, LonFacetsUtil.makeFacets(BLonElementType.u16, true, 0.0F, true, 365.0F, 1.0F, 0.0F, true, 8, 0, false, 0.0F, 1, null)
   );
   public static final Property JdayOfStartDST = newProperty(
      0, BLonFloat.DEFAULT, LonFacetsUtil.makeFacets(BLonElementType.u16, true, 1.0F, true, 365.0F, 1.0F, 0.0F, true, 8, 0, false, 0.0F, 1, null)
   );
   public static final Property MmonthOfStartDST = newProperty(
      0, BLonFloat.DEFAULT, LonFacetsUtil.makeFacets(BLonElementType.ub, true, 1.0F, true, 12.0F, 1.0F, 0.0F, true, 8, 4, false, 0.0F, 4, null)
   );
   public static final Property MweekOfStartDST = newProperty(
      0, BLonFloat.DEFAULT, LonFacetsUtil.makeFacets(BLonElementType.ub, true, 1.0F, true, 5.0F, 1.0F, 0.0F, true, 8, 1, false, 0.0F, 3, null)
   );
   public static final Property MdatedayOfStartDST = newProperty(0, BLonEnum.make(BLonDaysOfWeekEnum.daySun), LonFacetsUtil.makeFacets(BLonElementType.e8, 9));
   public static final Property hourOfEndDST = newProperty(0, BLonFloat.DEFAULT, LonFacetsUtil.makeFacets(BLonElementType.u8, 0.0F, 23.0F, 1.0F, 10, null));
   public static final Property minuteOfEndDST = newProperty(0, BLonFloat.DEFAULT, LonFacetsUtil.makeFacets(BLonElementType.u8, 0.0F, 59.0F, 1.0F, 11, null));
   public static final Property secondOfEndDST = newProperty(0, BLonFloat.DEFAULT, LonFacetsUtil.makeFacets(BLonElementType.u8, 0.0F, 59.0F, 1.0F, 12, null));
   public static final Property GdayOfEndDST = newProperty(
      0, BLonFloat.DEFAULT, LonFacetsUtil.makeFacets(BLonElementType.u16, true, 0.0F, true, 365.0F, 1.0F, 0.0F, true, 13, 0, false, 0.0F, 1, null)
   );
   public static final Property JdayOfEndDST = newProperty(
      0, BLonFloat.DEFAULT, LonFacetsUtil.makeFacets(BLonElementType.u16, true, 1.0F, true, 365.0F, 1.0F, 0.0F, true, 13, 0, false, 0.0F, 1, null)
   );
   public static final Property MmonthOfEndDST = newProperty(
      0, BLonFloat.DEFAULT, LonFacetsUtil.makeFacets(BLonElementType.ub, true, 1.0F, true, 12.0F, 1.0F, 0.0F, true, 13, 4, false, 0.0F, 4, null)
   );
   public static final Property MweekOfEndDST = newProperty(
      0, BLonFloat.DEFAULT, LonFacetsUtil.makeFacets(BLonElementType.ub, true, 1.0F, true, 5.0F, 1.0F, 0.0F, true, 13, 1, false, 0.0F, 3, null)
   );
   public static final Property MgatedayOfEndDST = newProperty(0, BLonEnum.make(BLonDaysOfWeekEnum.daySun), LonFacetsUtil.makeFacets(BLonElementType.e8, 14));
   public static final Type TYPE = Sys.loadType(BLonTimeZone.class);

   public BLonFloat getSecondTimeOffset() {
      return (BLonFloat)this.get(secondTimeOffset);
   }

   public void setSecondTimeOffset(BLonFloat v) {
      this.set(secondTimeOffset, v, null);
   }

   public BLonEnum getTypeOfDescription() {
      return (BLonEnum)this.get(typeOfDescription);
   }

   public void setTypeOfDescription(BLonEnum v) {
      this.set(typeOfDescription, v, null);
   }

   public BLonFloat getHourOfStartDST() {
      return (BLonFloat)this.get(hourOfStartDST);
   }

   public void setHourOfStartDST(BLonFloat v) {
      this.set(hourOfStartDST, v, null);
   }

   public BLonFloat getMinuteOfStartDST() {
      return (BLonFloat)this.get(minuteOfStartDST);
   }

   public void setMinuteOfStartDST(BLonFloat v) {
      this.set(minuteOfStartDST, v, null);
   }

   public BLonFloat getSecondOfStartDST() {
      return (BLonFloat)this.get(secondOfStartDST);
   }

   public void setSecondOfStartDST(BLonFloat v) {
      this.set(secondOfStartDST, v, null);
   }

   public BLonFloat getGdayOfStartDST() {
      return (BLonFloat)this.get(GdayOfStartDST);
   }

   public void setGdayOfStartDST(BLonFloat v) {
      this.set(GdayOfStartDST, v, null);
   }

   public BLonFloat getJdayOfStartDST() {
      return (BLonFloat)this.get(JdayOfStartDST);
   }

   public void setJdayOfStartDST(BLonFloat v) {
      this.set(JdayOfStartDST, v, null);
   }

   public BLonFloat getMmonthOfStartDST() {
      return (BLonFloat)this.get(MmonthOfStartDST);
   }

   public void setMmonthOfStartDST(BLonFloat v) {
      this.set(MmonthOfStartDST, v, null);
   }

   public BLonFloat getMweekOfStartDST() {
      return (BLonFloat)this.get(MweekOfStartDST);
   }

   public void setMweekOfStartDST(BLonFloat v) {
      this.set(MweekOfStartDST, v, null);
   }

   public BLonEnum getMdatedayOfStartDST() {
      return (BLonEnum)this.get(MdatedayOfStartDST);
   }

   public void setMdatedayOfStartDST(BLonEnum v) {
      this.set(MdatedayOfStartDST, v, null);
   }

   public BLonFloat getHourOfEndDST() {
      return (BLonFloat)this.get(hourOfEndDST);
   }

   public void setHourOfEndDST(BLonFloat v) {
      this.set(hourOfEndDST, v, null);
   }

   public BLonFloat getMinuteOfEndDST() {
      return (BLonFloat)this.get(minuteOfEndDST);
   }

   public void setMinuteOfEndDST(BLonFloat v) {
      this.set(minuteOfEndDST, v, null);
   }

   public BLonFloat getSecondOfEndDST() {
      return (BLonFloat)this.get(secondOfEndDST);
   }

   public void setSecondOfEndDST(BLonFloat v) {
      this.set(secondOfEndDST, v, null);
   }

   public BLonFloat getGdayOfEndDST() {
      return (BLonFloat)this.get(GdayOfEndDST);
   }

   public void setGdayOfEndDST(BLonFloat v) {
      this.set(GdayOfEndDST, v, null);
   }

   public BLonFloat getJdayOfEndDST() {
      return (BLonFloat)this.get(JdayOfEndDST);
   }

   public void setJdayOfEndDST(BLonFloat v) {
      this.set(JdayOfEndDST, v, null);
   }

   public BLonFloat getMmonthOfEndDST() {
      return (BLonFloat)this.get(MmonthOfEndDST);
   }

   public void setMmonthOfEndDST(BLonFloat v) {
      this.set(MmonthOfEndDST, v, null);
   }

   public BLonFloat getMweekOfEndDST() {
      return (BLonFloat)this.get(MweekOfEndDST);
   }

   public void setMweekOfEndDST(BLonFloat v) {
      this.set(MweekOfEndDST, v, null);
   }

   public BLonEnum getMgatedayOfEndDST() {
      return (BLonEnum)this.get(MgatedayOfEndDST);
   }

   public void setMgatedayOfEndDST(BLonEnum v) {
      this.set(MgatedayOfEndDST, v, null);
   }

   @Override
   public Type getType() {
      return TYPE;
   }

   @Override
   public void toOutputStream(LonOutputStream out) {
      this.primitiveToOutputStream(secondTimeOffset, out);
      this.primitiveToOutputStream(typeOfDescription, out);
      this.primitiveToOutputStream(hourOfStartDST, out);
      this.primitiveToOutputStream(minuteOfStartDST, out);
      this.primitiveToOutputStream(secondOfStartDST, out);
      int type = ((BLonCalendarTypeEnum)this.getTypeOfDescription().getEnum()).getOrdinal();
      if (type == 0) {
         this.primitiveToOutputStream(GdayOfStartDST, out);
      } else if (type == 1) {
         this.primitiveToOutputStream(JdayOfStartDST, out);
      } else {
         this.primitiveToOutputStream(MmonthOfStartDST, out);
         this.primitiveToOutputStream(MweekOfStartDST, out);
         this.primitiveToOutputStream(MdatedayOfStartDST, out);
      }

      this.primitiveToOutputStream(hourOfEndDST, out);
      this.primitiveToOutputStream(minuteOfEndDST, out);
      this.primitiveToOutputStream(secondOfEndDST, out);
      if (type == 0) {
         this.primitiveToOutputStream(GdayOfEndDST, out);
      } else if (type == 1) {
         this.primitiveToOutputStream(JdayOfEndDST, out);
      } else {
         this.primitiveToOutputStream(MmonthOfEndDST, out);
         this.primitiveToOutputStream(MweekOfEndDST, out);
         this.primitiveToOutputStream(MgatedayOfEndDST, out);
      }
   }

   @Override
   public void fromInputStream(LonInputStream in) {
      this.primitiveFromInputStream(secondTimeOffset, in);
      this.primitiveFromInputStream(typeOfDescription, in);
      this.primitiveFromInputStream(hourOfStartDST, in);
      this.primitiveFromInputStream(minuteOfStartDST, in);
      this.primitiveFromInputStream(secondOfStartDST, in);
      int type = ((BLonCalendarTypeEnum)this.getTypeOfDescription().getEnum()).getOrdinal();
      if (type == 0) {
         this.primitiveFromInputStream(GdayOfStartDST, in);
      } else if (type == 1) {
         this.primitiveFromInputStream(JdayOfStartDST, in);
      } else {
         this.primitiveFromInputStream(MmonthOfStartDST, in);
         this.primitiveFromInputStream(MweekOfStartDST, in);
         this.primitiveFromInputStream(MdatedayOfStartDST, in);
      }

      this.primitiveFromInputStream(hourOfEndDST, in);
      this.primitiveFromInputStream(minuteOfEndDST, in);
      this.primitiveFromInputStream(secondOfEndDST, in);
      if (type == 0) {
         this.primitiveFromInputStream(GdayOfEndDST, in);
      } else if (type == 1) {
         this.primitiveFromInputStream(JdayOfEndDST, in);
      } else {
         this.primitiveFromInputStream(MmonthOfEndDST, in);
         this.primitiveFromInputStream(MweekOfEndDST, in);
         this.primitiveFromInputStream(MgatedayOfEndDST, in);
      }
   }
}
