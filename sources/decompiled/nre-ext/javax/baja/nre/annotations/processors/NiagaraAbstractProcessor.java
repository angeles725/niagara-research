package javax.baja.nre.annotations.processors;

import java.io.IOException;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import javax.tools.Diagnostic.Kind;

public abstract class NiagaraAbstractProcessor extends AbstractProcessor {
   protected Messager msg;
   protected Elements elements;
   protected Types types;
   protected static final boolean ERROR = true;
   private String cachedModulePath = null;
   private String providedModuleRoot = null;

   @Override
   public void init(ProcessingEnvironment processingEnv) {
      super.init(processingEnv);
      this.msg = processingEnv.getMessager();
      this.elements = processingEnv.getElementUtils();
      this.types = processingEnv.getTypeUtils();
      if (processingEnv.getOptions().containsKey("niagara.module.root")) {
         this.providedModuleRoot = processingEnv.getOptions().get("niagara.module.root");
      }
   }

   public String getModuleRootPath(Element elem) {
      if (this.providedModuleRoot != null) {
         return this.providedModuleRoot;
      }

      if (this.cachedModulePath == null
         || !this.cachedModulePath.contains(elem.getSimpleName())
         || !this.cachedModulePath.contains(elem.getEnclosingElement().toString().replaceAll("\\.", "/"))) {
         try {
            Filer filer = this.processingEnv.getFiler();
            String fileName = "pathAccess" + elem.getEnclosingElement().toString() + "." + elem.getSimpleName() + ".xml";
            FileObject fo = filer.createResource(StandardLocation.SOURCE_OUTPUT, elem.getEnclosingElement().toString(), fileName, (Element[])null);
            this.cachedModulePath = fo.toUri().getPath();
            String elementPath = elem.getEnclosingElement().toString().replace('.', '/') + '/' + fileName;
            int indexElementPath = this.cachedModulePath.lastIndexOf(elementPath);
            if (indexElementPath > 0) {
               this.cachedModulePath = this.cachedModulePath.substring(0, indexElementPath);
            }
         } catch (IOException ioe) {
            ioe.printStackTrace();
            this.msg.printMessage(Kind.ERROR, "Unable to find the module root folder for " + elem.getSimpleName() + ": " + ioe.getMessage());
            return null;
         }
      }

      int indexBuildDir = this.cachedModulePath.lastIndexOf("/build");
      return indexBuildDir > 0 ? this.cachedModulePath.substring(0, indexBuildDir) : this.cachedModulePath;
   }
}
