package javax.baja.nre.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Repeatable(NiagaraProperties.class)
public @interface NiagaraProperty {
   String name();

   String type();

   Facet[] facets() default {};

   int flags() default 0;

   String defaultValue() default "";

   boolean override() default false;

   boolean deprecated() default false;
}
