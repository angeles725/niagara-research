package javax.baja.nre.annotations.processors.slot;

import javax.baja.nre.annotations.NiagaraProperty;

public class PropertyAnnotation implements SlotAnnotation<NiagaraProperty> {
   private final NiagaraProperty annotation;

   public PropertyAnnotation(NiagaraProperty annotation) {
      this.annotation = annotation;
   }

   @Override
   public String getName() {
      return this.annotation.name();
   }

   @Override
   public boolean getOverride() {
      return this.annotation.override();
   }
}
