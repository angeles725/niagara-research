package javax.baja.nre.annotations.processors.slot;

import javax.baja.nre.annotations.NiagaraAction;

public class ActionAnnotation implements SlotAnnotation<NiagaraAction> {
   private final NiagaraAction annotation;

   public ActionAnnotation(NiagaraAction annotation) {
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
