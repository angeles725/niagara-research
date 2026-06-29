package javax.baja.lonworks.proxy;

import javax.baja.control.BControlPoint;
import javax.baja.lonworks.londata.BLonData;
import javax.baja.lonworks.londata.BLonElementQualifiers;
import javax.baja.lonworks.londata.BLonPrimitive;
import javax.baja.lonworks.londata.LonFacetsUtil;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.status.BStatusNumeric;
import javax.baja.status.BStatusValue;
import javax.baja.sys.BFacets;
import javax.baja.sys.BFloat;
import javax.baja.sys.BInteger;
import javax.baja.sys.BNumber;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.units.BUnit;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "readValue",
      type = "BStatusNumeric",
      defaultValue = "new BStatusNumeric()",
      flags = 3,
      override = true
   ), @NiagaraProperty(
      name = "writeValue",
      type = "BStatusNumeric",
      defaultValue = "new BStatusNumeric()",
      flags = 3,
      override = true
   )})
public final class BLonFloatProxyExt extends BLonProxyExt {
   public static final Property readValue = newProperty(3, new BStatusNumeric(), null);
   public static final Property writeValue = newProperty(3, new BStatusNumeric(), null);
   public static final Type TYPE = Sys.loadType(BLonFloatProxyExt.class);
   private boolean nanIsNull = false;
   private boolean nullSpecified = false;
   private double nullVal;

   @Override
   public Type getType() {
      return TYPE;
   }

   @Override
   protected void extStarted() {
      super.extStarted();
      this.checkFacets();
   }

   public void pointFacetsChanged() {
      super.pointFacetsChanged();
      this.checkFacets();
      BControlPoint cp = (BControlPoint)this.getParent();
      BFacets pntFcts = cp.getFacets();
      BUnit pntUnit = (BUnit)pntFcts.get("units");
      if (pntUnit != null) {
         BFacets devFcts = this.getDeviceFacets();
         BUnit devUnit = (BUnit)devFcts.get("units");
         if (devUnit != null) {
            if (pntUnit.isConvertible(devUnit)) {
               BNumber curDevMin = (BNumber)devFcts.get("min", BFloat.NEGATIVE_INFINITY);
               double devMin;
               if (curDevMin != BFloat.NEGATIVE_INFINITY) {
                  devMin = devUnit.convertTo(pntUnit, curDevMin.getNumeric());
               } else {
                  devMin = Double.NEGATIVE_INFINITY;
               }

               BNumber curDevMax = (BNumber)devFcts.get("max", BFloat.POSITIVE_INFINITY);
               double devMax;
               if (curDevMax != BFloat.POSITIVE_INFINITY) {
                  devMax = (float)devUnit.convertTo(pntUnit, curDevMax.getNumeric());
               } else {
                  devMax = Double.POSITIVE_INFINITY;
               }

               double newMin = ((BNumber)pntFcts.get("min", BFloat.NEGATIVE_INFINITY)).getDouble();
               double newMax = ((BNumber)pntFcts.get("max", BFloat.POSITIVE_INFINITY)).getDouble();
               double min = Math.max(newMin, devMin);
               double max = Math.min(newMax, devMax);
               if (min != newMin || max != newMax) {
                  BInteger p = (BInteger)devFcts.get("precision", null);
                  int prec = p == null ? 1 : p.getInt();
                  BFacets f = BFacets.makeNumeric(pntUnit, prec, min, max);
                  cp.setFacets(f);
               }
            }
         }
      }
   }

   @Override
   public BStatusValue getStatusValue(BLonPrimitive newValue) {
      double num = newValue.getDataAsDouble();
      BStatusNumeric sNum = new BStatusNumeric(num);
      if (Double.isNaN(num) && this.nanIsNull) {
         sNum.setStatusNull(true);
      } else if (this.nullSpecified && num == this.nullVal) {
         sNum.setStatusNull(true);
      }

      return sNum;
   }

   @Override
   public BLonPrimitive getPrimitiveValue(BStatusValue value) {
      BLonData dataPoint = this.getDataPoint();
      double val = ((BStatusNumeric)value).getValue();
      BLonElementQualifiers e = LonFacetsUtil.getQualifiers(this.targetProp.getFacets());
      return ((BLonPrimitive)dataPoint.get(this.targetProp)).makeFromDouble(val, e);
   }

   private void checkFacets() {
      BControlPoint cp = (BControlPoint)this.getParent();
      BFacets pntFcts = cp.getFacets();
      this.nanIsNull = pntFcts.getb("nanIsNull", false);
      double d = pntFcts.getd("isNull", Double.NaN);
      if (Double.isNaN(d)) {
         this.nullSpecified = false;
      } else {
         this.nullSpecified = true;
         this.nullVal = d;
      }
   }
}
