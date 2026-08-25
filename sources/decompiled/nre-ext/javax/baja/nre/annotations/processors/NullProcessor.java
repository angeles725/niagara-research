package javax.baja.nre.annotations.processors;

import com.google.auto.service.AutoService;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;

@SupportedAnnotationTypes(
   {
         "javax.baja.nre.annotations.NiagaraEnum",
         "javax.baja.nre.annotations.NiagaraSingleton",
         "javax.baja.nre.annotations.NiagaraSlots",
         "javax.baja.rpc.NiagaraRpc",
         "javax.baja.nre.annotations.NoSlotomatic"
   }
)
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@AutoService(Processor.class)
public class NullProcessor extends AbstractProcessor {
   @Override
   public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
      return true;
   }
}
