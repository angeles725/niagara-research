package javax.baja.nre.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Repeatable(NiagaraActions.class)
public @interface NiagaraAction {
   String name();

   String parameterType() default "";

   String returnType() default "";

   Facet[] facets() default {};

   int flags() default 0;

   String defaultValue() default "";

   boolean override() default false;

   boolean deprecated() default false;
}
