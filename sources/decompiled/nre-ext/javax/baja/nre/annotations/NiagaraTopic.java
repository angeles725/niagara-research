package javax.baja.nre.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Repeatable(NiagaraTopics.class)
public @interface NiagaraTopic {
   String name();

   String eventType() default "";

   Facet[] facets() default {};

   int flags() default 0;

   boolean override() default false;

   boolean deprecated() default false;
}
