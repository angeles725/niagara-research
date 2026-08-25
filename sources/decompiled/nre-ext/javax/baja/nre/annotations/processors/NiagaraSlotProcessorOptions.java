package javax.baja.nre.annotations.processors;

import javax.annotation.processing.ProcessingEnvironment;
import javax.tools.Diagnostic.Kind;

public class NiagaraSlotProcessorOptions {
   private final Kind warningKind;

   public NiagaraSlotProcessorOptions(ProcessingEnvironment processingEnvironment) {
      String warningKindOption = processingEnvironment.getOptions().getOrDefault("niagara.slot.warning.level", "WARNING");
      this.warningKind = Kind.valueOf(warningKindOption);
   }

   public Kind getWarningKind() {
      return this.warningKind;
   }
}
