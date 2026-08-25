package javax.baja.nre.annotations.processors.slot;

import java.lang.annotation.Annotation;

public interface SlotAnnotation<T extends Annotation> {
   String getName();

   boolean getOverride();
}
