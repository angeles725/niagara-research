package javax.baja.nre.annotations;

public @interface Range {
   String value();

   int ordinal() default -1;
}
