package javax.baja.nre.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
public @interface NiagaraProperties {
   NiagaraProperty[] value();
}
