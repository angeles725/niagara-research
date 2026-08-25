package javax.baja.nre.annotations.processors.slot;

import javax.baja.nre.annotations.NiagaraTopic;

public class TopicAnnotation implements SlotAnnotation<NiagaraTopic> {
   private final NiagaraTopic annotation;

   public TopicAnnotation(NiagaraTopic annotation) {
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
