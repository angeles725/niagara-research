package javax.baja.bacnet.point;

import javax.baja.driver.point.BTuningPolicyMap;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BComponent;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperty(
   name = "defaultPolicy",
   type = "BBacnetTuningPolicy",
   defaultValue = "new BBacnetTuningPolicy()",
   override = true
)
public class BBacnetTuningPolicyMap extends BTuningPolicyMap {
   public static final Property defaultPolicy = newProperty(0, new BBacnetTuningPolicy(), null);
   public static final Type TYPE = Sys.loadType(BBacnetTuningPolicyMap.class);

   public Type getType() {
      return TYPE;
   }

   public boolean isChildLegal(BComponent child) {
      return child instanceof BBacnetTuningPolicy;
   }
}
