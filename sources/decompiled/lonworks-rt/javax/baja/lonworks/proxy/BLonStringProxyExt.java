package javax.baja.lonworks.proxy;

import javax.baja.lonworks.londata.BLonData;
import javax.baja.lonworks.londata.BLonPrimitive;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.status.BStatusString;
import javax.baja.status.BStatusValue;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public final class BLonStringProxyExt extends BLonProxyExt {
   public static final Type TYPE = Sys.loadType(BLonStringProxyExt.class);

   @Override
   public Type getType() {
      return TYPE;
   }

   @Override
   public BStatusValue getStatusValue(BLonPrimitive newValue) {
      return new BStatusString(newValue.getDataAsString());
   }

   @Override
   public BLonPrimitive getPrimitiveValue(BStatusValue value) {
      BLonData dataPoint = this.getDataPoint();
      String val = ((BStatusString)value).getValue();
      return ((BLonPrimitive)dataPoint.get(this.targetProp)).makeFromString(val);
   }
}
