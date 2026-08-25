package javax.baja.nre.annotations;

public @interface NiagaraEnum {
   Range[] range();

   String defaultValue() default "";
}
