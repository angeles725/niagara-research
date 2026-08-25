package javax.baja.nre.annotations.processors;

import com.google.auto.service.AutoService;
import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.annotation.processing.SupportedSourceVersion;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraActions;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraTopic;
import javax.baja.nre.annotations.NiagaraTopics;
import javax.baja.nre.annotations.processors.slot.BajaUnit;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;

@SupportedAnnotationTypes(
   {
         "javax.baja.nre.annotations.NiagaraAction",
         "javax.baja.nre.annotations.NiagaraActions",
         "javax.baja.nre.annotations.NiagaraProperty",
         "javax.baja.nre.annotations.NiagaraProperties",
         "javax.baja.nre.annotations.NiagaraTopic",
         "javax.baja.nre.annotations.NiagaraTopics"
   }
)
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedOptions("niagara.slot.warning.level")
@AutoService(Processor.class)
public class NiagaraSlotProcessor extends AbstractProcessor {
   private NiagaraSlotProcessorOptions options = null;

   @Override
   public synchronized void init(ProcessingEnvironment processingEnv) {
      super.init(processingEnv);
      this.options = new NiagaraSlotProcessorOptions(processingEnv);
   }

   @Override
   public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
      for (TypeElement element : getElementsAnnotatedWith(
         roundEnv, NiagaraProperty.class, NiagaraProperties.class, NiagaraAction.class, NiagaraActions.class, NiagaraTopic.class, NiagaraTopics.class
      )) {
         BajaUnit unit = new BajaUnit(this.processingEnv, this.options, element);
         unit.checkProperties();
         unit.checkActions();
         unit.checkTopics();
      }

      return true;
   }

   @SafeVarargs
   private static Set<TypeElement> getElementsAnnotatedWith(RoundEnvironment roundEnv, Class<? extends Annotation>... classes) {
      Set<TypeElement> elements = new HashSet<>();

      for (Class<? extends Annotation> cls : classes) {
         elements.addAll(
            roundEnv.getElementsAnnotatedWith(cls)
               .stream()
               .flatMap(element -> element instanceof TypeElement ? Stream.of((Element)element) : Stream.empty())
               .collect(Collectors.toSet())
         );
      }

      return elements;
   }
}
