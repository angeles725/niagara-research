package javax.baja.lonworks.proxy;

import javax.baja.lonworks.londata.BLonData;
import javax.baja.lonworks.londata.BLonPrimitive;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.status.BStatusBoolean;
import javax.baja.status.BStatusValue;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "readValue",
      type = "BStatusBoolean",
      defaultValue = "new BStatusBoolean()",
      flags = 3,
      override = true
   ), @NiagaraProperty(
      name = "writeValue",
      type = "BStatusBoolean",
      defaultValue = "new BStatusBoolean()",
      flags = 3,
      override = true
   )})
public final class BLonBooleanProxyExt extends BLonProxyExt {
   public static final Property readValue = newProperty(3, new BStatusBoolean(), null);
   public static final Property writeValue = newProperty(3, new BStatusBoolean(), null);
   public static final Type TYPE = Sys.loadType(BLonBooleanProxyExt.class);

   @Override
   public Type getType() {
      return TYPE;
   }

   @Override
   public BStatusValue getStatusValue(BLonPrimitive newValue) {
      return new BStatusBoolean(newValue.getDataAsBoolean());
   }

   @Override
   public BLonPrimitive getPrimitiveValue(BStatusValue value) {
      BLonData dataPoint = this.getDataPoint();
      boolean val = ((BStatusBoolean)value).getValue();
      return ((BLonPrimitive)dataPoint.get(this.targetProp)).makeFromBoolean(val);
   }
}
