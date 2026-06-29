package javax.baja.lonworks.proxy;

import javax.baja.control.BControlPoint;
import javax.baja.control.BEnumPoint;
import javax.baja.data.BIDataValue;
import javax.baja.lonworks.londata.BLonData;
import javax.baja.lonworks.londata.BLonEnum;
import javax.baja.lonworks.londata.BLonPrimitive;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.status.BStatusEnum;
import javax.baja.status.BStatusValue;
import javax.baja.sys.BDynamicEnum;
import javax.baja.sys.BEnum;
import javax.baja.sys.BEnumRange;
import javax.baja.sys.BFacets;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "readValue",
      type = "BStatusEnum",
      defaultValue = "new BStatusEnum()",
      flags = 3,
      override = true
   ), @NiagaraProperty(
      name = "writeValue",
      type = "BStatusEnum",
      defaultValue = "new BStatusEnum()",
      flags = 3,
      override = true
   )})
public final class BLonEnumProxyExt extends BLonProxyExt {
   public static final Property readValue = newProperty(3, new BStatusEnum(), null);
   public static final Property writeValue = newProperty(3, new BStatusEnum(), null);
   public static final Type TYPE = Sys.loadType(BLonEnumProxyExt.class);
   private boolean nullSpecified = false;
   private int nullVal;

   @Override
   public Type getType() {
      return TYPE;
   }

   @Override
   protected void extStarted() {
      this.checkFacets();
      this.updateFacets();
      BEnumRange range = (BEnumRange)this.getDeviceFacets().getFacet("range");
      if (range != null && range.getOrdinals().length != 0) {
         BDynamicEnum en = BDynamicEnum.make(range.getOrdinals()[0], range);
         BLonPrimitive tgt = this.getTarget();
         BEnum v = tgt.getDataAsEnum(en);
         this.setReadValue(new BStatusEnum(v));
      } else {
         this.getDevice().getLogger().warning("Invalid range for enum proxy in " + this.getDevice().getDisplayName(null) + ":" + this.getDisplayName(null));
      }
   }

   public void pointFacetsChanged() {
      this.updateFacets();
      super.pointFacetsChanged();
      this.checkFacets();
   }

   private void updateFacets() {
      BFacets f = ((BControlPoint)this.getParent()).getFacets();
      BFacets DeviceFacets = this.getDeviceFacets();
      BIDataValue idv = (BIDataValue)f.get("range");
      if (idv != null) {
         this.setDeviceFacets(BFacets.make(DeviceFacets, "range", idv));
      }
   }

   @Override
   protected void deviceFacetsChanged() {
      BLonPrimitive tgt = this.getTarget();
      if (tgt != null && tgt.getType().is(BLonEnum.TYPE)) {
         BEnumRange rng = ((BLonEnum)tgt).getEnum().getRange();
         BFacets cpFacets = ((BControlPoint)this.getParent()).getFacets();
         BFacets newFacets = BFacets.make(cpFacets, "range", rng);
         ((BControlPoint)this.getParent()).setFacets(newFacets);
      }
   }

   @Override
   public BStatusValue getStatusValue(BLonPrimitive newValue) {
      BStatusEnum msElem = (BStatusEnum)((BEnumPoint)this.getParent()).getOutStatusValue();
      BEnum v = newValue.getDataAsEnum(msElem.getValue());
      BStatusEnum sEnum = new BStatusEnum(v);
      if (this.nullSpecified && v.getOrdinal() == this.nullVal) {
         sEnum.setStatusNull(true);
      }

      return sEnum;
   }

   @Override
   public BLonPrimitive getPrimitiveValue(BStatusValue value) {
      BLonData dataPoint = this.getDataPoint();
      BDynamicEnum val = ((BStatusEnum)value).getValue();
      return ((BLonPrimitive)dataPoint.get(this.targetProp)).makeFromEnum(val);
   }

   private void checkFacets() {
      BControlPoint cp = (BControlPoint)this.getParent();
      BFacets pntFcts = cp.getFacets();
      int n = pntFcts.geti("isNull", Integer.MAX_VALUE);
      if (n == Integer.MAX_VALUE) {
         this.nullSpecified = false;
      } else {
         this.nullSpecified = true;
         this.nullVal = n;
      }
   }
}
