package javax.baja.nre.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface NiagaraType {
   AgentOn[] agent() default {};

   Adapter adapter() default @Adapter(from = "", to = "");

   FileExt[] ext() default {};

   String ordScheme() default "";
}
