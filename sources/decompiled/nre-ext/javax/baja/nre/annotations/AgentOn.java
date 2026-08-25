package javax.baja.nre.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface AgentOn {
   String[] types();

   String app() default "";

   AgentOn.Preference defaultAgent() default AgentOn.Preference.NORMAL;

   String requiredPermissions() default "";

   enum Preference {
      PREFERRED,
      NORMAL,
      NOT_PREFERRED;
   }
}
