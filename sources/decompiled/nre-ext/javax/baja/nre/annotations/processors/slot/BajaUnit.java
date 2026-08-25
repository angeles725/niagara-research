package javax.baja.nre.annotations.processors.slot;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraTopic;
import javax.baja.nre.annotations.processors.NiagaraSlotProcessorOptions;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic.Kind;

public class BajaUnit {
   private final List<VariableElement> allSlots;
   private final List<VariableElement> parentSlots;
   private final List<VariableElement> unitSlots;
   private final TypeElement element;
   private final Types types;
   private final Messager msg;
   private final Elements elements;
   private final TypeElement property;
   private final TypeElement action;
   private final TypeElement topic;
   private final Kind warningKind;

   public BajaUnit(ProcessingEnvironment processingEnvironment, NiagaraSlotProcessorOptions options, TypeElement element) {
      Objects.requireNonNull(options);
      this.element = element;
      this.types = processingEnvironment.getTypeUtils();
      this.elements = processingEnvironment.getElementUtils();
      this.msg = processingEnvironment.getMessager();
      this.property = this.elements.getTypeElement("javax.baja.sys.Property");
      this.action = this.elements.getTypeElement("javax.baja.sys.Action");
      this.topic = this.elements.getTypeElement("javax.baja.sys.Topic");
      this.warningKind = options.getWarningKind();
      this.allSlots = ElementFilter.fieldsIn(this.elements.getAllMembers(element))
         .stream()
         .filter(
            e -> {
               TypeMirror eType = e.asType();
               return this.types.isSameType(this.property.asType(), eType)
                  || this.types.isSameType(this.action.asType(), eType)
                  || this.types.isSameType(this.topic.asType(), eType);
            }
         )
         .collect(Collectors.toList());
      this.parentSlots = this.allSlots.stream().filter(e -> e.getEnclosingElement() != element).collect(Collectors.toList());
      this.unitSlots = this.allSlots.stream().filter(e -> e.getEnclosingElement() == element).collect(Collectors.toList());
   }

   public void checkProperties() {
      this.checkSlots(NiagaraProperty.class, this.property.asType());
   }

   public void checkActions() {
      this.checkSlots(NiagaraAction.class, this.action.asType());
   }

   public void checkTopics() {
      this.checkSlots(NiagaraTopic.class, this.topic.asType());
   }

   private void checkSlots(Class<? extends Annotation> slotAnnotation, TypeMirror annotationType) {
      List<SlotAnnotation<?>> slots = Arrays.stream(this.element.getAnnotationsByType(slotAnnotation))
         .map(BajaUnit::mapAnnotation)
         .collect(Collectors.toList());
      if (!slots.isEmpty()) {
         List<String> parentSlotNames = this.parentSlots
            .stream()
            .filter(it -> this.types.isSameType(annotationType, it.asType()))
            .map(it -> it.getSimpleName().toString())
            .collect(Collectors.toList());
         List<String> unitSlotNames = this.unitSlots
            .stream()
            .filter(it -> this.types.isSameType(annotationType, it.asType()))
            .map(it -> it.getSimpleName().toString())
            .collect(Collectors.toList());

         for (SlotAnnotation<?> slot : slots) {
            if (!unitSlotNames.contains(slot.getName())) {
               this.msg
                  .printMessage(
                     Kind.ERROR,
                     String.format("Slot with name %s not found on class %s; have you run slot-o-matic?", slot.getName(), this.element.asType().toString())
                  );
            }

            if (slot.getOverride()) {
               if (!parentSlotNames.contains(slot.getName())) {
                  this.msg
                     .printMessage(
                        this.warningKind,
                        String.format("Slot %s in class %s does not override a slot in its parent class(es)", slot.getName(), this.element.asType().toString())
                     );
               }
            } else if (parentSlotNames.contains(slot.getName())) {
               this.msg
                  .printMessage(
                     this.warningKind, String.format("Missing 'override = true' for slot %s on class %s", slot.getName(), this.element.asType().toString())
                  );
            }
         }
      }
   }

   private static <T extends Annotation> SlotAnnotation<?> mapAnnotation(T annotation) {
      Class<?> annotationClass = annotation.getClass();
      if (NiagaraProperty.class.isAssignableFrom(annotationClass)) {
         return new PropertyAnnotation((NiagaraProperty)annotation);
      } else if (NiagaraAction.class.isAssignableFrom(annotationClass)) {
         return new ActionAnnotation((NiagaraAction)annotation);
      } else if (NiagaraTopic.class.isAssignableFrom(annotationClass)) {
         return new TopicAnnotation((NiagaraTopic)annotation);
      } else {
         throw new IllegalArgumentException("Could not find model for " + annotationClass.getCanonicalName());
      }
   }
}
