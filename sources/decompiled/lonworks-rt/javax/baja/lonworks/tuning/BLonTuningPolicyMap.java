package javax.baja.lonworks.tuning;

import javax.baja.driver.point.BTuningPolicy;
import javax.baja.driver.point.BTuningPolicyMap;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BComponent;
import javax.baja.sys.BRelTime;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "defaultProxyPolicy",
      type = "BTuningPolicy",
      defaultValue = "new BLonTuningPolicy(BRelTime.make(0),BRelTime.make(0),true,true,true,BRelTime.make(0))",
      flags = 5
   ), @NiagaraProperty(
      name = "defaultPolicy",
      type = "BLonTuningPolicy",
      defaultValue = "new BLonTuningPolicy(BRelTime.make(0),BRelTime.make(60000),true,true,true,BRelTime.make(0))",
      override = true
   )})
public class BLonTuningPolicyMap extends BTuningPolicyMap {
   public static final Property defaultProxyPolicy = newProperty(
      5, new BLonTuningPolicy(BRelTime.make(0L), BRelTime.make(0L), true, true, true, BRelTime.make(0L)), null
   );
   public static final Property defaultPolicy = newProperty(
      0, new BLonTuningPolicy(BRelTime.make(0L), BRelTime.make(60000L), true, true, true, BRelTime.make(0L)), null
   );
   public static final Type TYPE = Sys.loadType(BLonTuningPolicyMap.class);

   public BTuningPolicy getDefaultProxyPolicy() {
      return (BTuningPolicy)this.get(defaultProxyPolicy);
   }

   public void setDefaultProxyPolicy(BTuningPolicy v) {
      this.set(defaultProxyPolicy, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public boolean isChildLegal(BComponent child) {
      return child instanceof BLonTuningPolicy;
   }
}
