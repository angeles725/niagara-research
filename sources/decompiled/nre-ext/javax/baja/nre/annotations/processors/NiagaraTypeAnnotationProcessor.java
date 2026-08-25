package javax.baja.nre.annotations.processors;

import com.google.auto.service.AutoService;
import java.io.File;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.annotation.processing.SupportedSourceVersion;
import javax.baja.nre.annotations.NiagaraType;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.QualifiedNameable;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic.Kind;

@SupportedAnnotationTypes("javax.baja.nre.annotations.NiagaraType")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedOptions({"niagara.module.root", "niagara.test.roots"})
@AutoService(Processor.class)
public class NiagaraTypeAnnotationProcessor extends NiagaraAbstractProcessor {
   private static final Pattern DOT_PATTERN = Pattern.compile("\\.");
   private final Map<String, Set<String>> testRoots = new LinkedHashMap<>();

   private static Set<String> setOf(String... strings) {
      Set<String> set = new LinkedHashSet<>();
      set.addAll(Arrays.asList(strings));
      return set;
   }

   @Override
   public void init(ProcessingEnvironment processingEnv) {
      super.init(processingEnv);
      this.testRoots.put("srcTest", setOf("java"));
      if (processingEnv.getOptions().containsKey("niagara.test.roots")) {
         String roots = processingEnv.getOptions().get("niagara.test.roots");

         for (String rootPair : roots.split(";")) {
            String[] pair = rootPair.split("=");
            if (pair.length != 2) {
               this.msg.printMessage(Kind.WARNING, "Invalid test root string " + rootPair);
            } else {
               this.testRoots.computeIfAbsent(pair[0], k -> new LinkedHashSet<>()).add(pair[1]);
            }
         }
      }
   }

   @Override
   public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
      TypeElement bIObject = this.elements.getTypeElement("javax.baja.sys.BIObject");
      TypeElement bTest = this.elements.getTypeElement("javax.baja.test.BTest");

      for (Element elem : roundEnv.getElementsAnnotatedWith(NiagaraType.class)) {
         NiagaraType annotation = elem.getAnnotation(NiagaraType.class);
         String typeName = elem.getSimpleName().toString();
         if (!this.types.isSubtype(elem.asType(), bIObject.asType())) {
            if (elem.getKind() != ElementKind.CLASS) {
               typeName = ((QualifiedNameable)elem).getQualifiedName().toString();
            }

            this.msg.printMessage(Kind.ERROR, "The type " + typeName + " was annotated with NiagaraType, but is not a BIObject.");
            return true;
         }

         String typeClass = elem.getEnclosingElement() + "." + elem.getSimpleName();
         if (typeName.charAt(0) != 'B') {
            this.msg.printMessage(Kind.ERROR, "The type " + typeClass + " does not start with B. All Niagara types must start with B.");
            return true;
         }

         typeName = typeName.substring(1);
         String rootPath = this.getModuleRootPath(elem);
         String xmlPath;
         if (bTest != null && this.isTestClass(rootPath, typeClass)) {
            xmlPath = rootPath + "/moduleTest-include.xml";
         } else {
            xmlPath = rootPath + "/module-include.xml";
         }

         ModuleInclude moduleInclude;
         try {
            moduleInclude = new ModuleInclude(xmlPath);
         } catch (ModuleInclude.XmlException e) {
            this.msg.printMessage(Kind.ERROR, e.getMessage());
            return true;
         }

         if (!moduleInclude.containsEntry(typeName, typeClass) || !moduleInclude.entryMatches(annotation, typeName, typeClass)) {
            moduleInclude.addTypeNode(annotation, typeName, typeClass);

            try {
               moduleInclude.stripWhitespace();
               moduleInclude.format();
               moduleInclude.save(xmlPath);
            } catch (ModuleInclude.XmlException e) {
               this.msg.printMessage(Kind.ERROR, e.getMessage());
               return true;
            }
         }
      }

      return true;
   }

   private boolean isTestClass(String moduleRoot, String typeClass) {
      String fileBase = DOT_PATTERN.matcher(typeClass).replaceAll(Matcher.quoteReplacement(File.separator));

      for (Entry<String, Set<String>> testRoot : this.testRoots.entrySet()) {
         String testFolder = testRoot.getKey();

         for (String extension : testRoot.getValue()) {
            String filePath = (moduleRoot + '/' + testFolder + '/' + fileBase + '.' + extension).replace('/', File.separatorChar);
            if (new File(filePath).exists()) {
               return true;
            }
         }
      }

      return false;
   }
}
