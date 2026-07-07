package com.tridium.template.api.impl;

import com.tridium.template.BTemplateConfig;
import com.tridium.template.api.TemplateSourceType;
import com.tridium.template.api.TemplateValueSource;
import com.tridium.template.file.BNtplFile;
import java.util.Objects;
import java.util.stream.IntStream;
import javax.baja.naming.BOrd;
import javax.baja.naming.UnresolvedException;
import javax.baja.nre.util.Array;
import javax.baja.sys.BComponent;

public class FileTemplateSource extends TemplateSourceWithConfig {
   private final BComponent baseComponent;
   private final String basePath;
   private BNtplFile templateFile;
   private BTemplateConfig config;
   private final Array<BOrd> optionalsOrds;

   private FileTemplateSource(BNtplFile templateFile) {
      this.templateFile = templateFile;
      this.templateFile.open();
      this.baseComponent = templateFile.getBaseComponent();
      this.basePath = this.baseComponent.getSlotPath().getBody();
      this.optionalsOrds = this.templateFile.getTemplateManifest().optional;
   }

   public static FileTemplateSource load(BNtplFile templateFile) {
      Objects.requireNonNull(templateFile);
      return new FileTemplateSource(templateFile);
   }

   @Override
   protected TemplateValueSource getValueSource() {
      return TemplateValueSource.DEFAULT_VALUE;
   }

   @Override
   protected BComponent getBase() {
      return this.templateFile.getBaseComponent();
   }

   @Override
   protected BTemplateConfig getConfig() {
      if (this.config == null) {
         this.config = BTemplateConfig.getConfigForRoot(this.baseComponent, this.templateFile.getTemplateManifest().isApplication);
      }

      return this.config;
   }

   @Override
   public String getFileName() {
      return this.templateFile.getFileName();
   }

   @Override
   public TemplateSourceType getSourceType() {
      return TemplateSourceType.FILE;
   }

   @Override
   public IntStream getOptionalComponentKeyStream() {
      return IntStream.range(0, this.optionalsOrds.size()).filter(this::optionalComponentExists);
   }

   @Override
   public String getOptionalComponentPath(int optionalComponentKey) {
      BComponent optionalComponent = this.getOptionalComponent(optionalComponentKey);
      if (optionalComponent == null) {
         return null;
      } else {
         String body = optionalComponent.getSlotPath().getBody();
         int basePathIndex = body.indexOf(this.basePath);
         if (basePathIndex < 0) {
            return null;
         } else {
            String path = body.substring(basePathIndex + this.basePath.length());
            return path.startsWith("/") ? path.substring(1) : path;
         }
      }
   }

   @Override
   public String getOptionalComponentNType(int optionalComponentKey) {
      BComponent optionalComponent = this.getOptionalComponent(optionalComponentKey);
      return optionalComponent == null ? null : optionalComponent.getType().toString();
   }

   @Override
   public boolean isOptionalInstalled(int optionalComponentKey) {
      return this.optionalComponentExists(optionalComponentKey);
   }

   @Override
   public void close() {
      if (this.templateFile != null && this.templateFile.isOpen()) {
         this.templateFile.close();
         this.templateFile = null;
      }

      super.close();
   }

   private boolean optionalComponentExists(int optionalComponentKey) {
      return this.getOptionalComponent(optionalComponentKey) != null;
   }

   private BComponent getOptionalComponent(int optionalComponentKey) {
      BComponent result = null;

      try {
         BOrd optionalOrd = (BOrd)this.optionalsOrds.get(optionalComponentKey);
         result = (BComponent)optionalOrd.resolve(this.baseComponent).get();
      } catch (UnresolvedException var4) {
      }

      return result;
   }
}
