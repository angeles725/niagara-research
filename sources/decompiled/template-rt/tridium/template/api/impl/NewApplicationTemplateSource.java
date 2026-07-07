package com.tridium.template.api.impl;

import com.tridium.template.api.TemplateType;
import com.tridium.template.application.ApplicationTemplateUtil;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.baja.file.BDataFile;
import javax.baja.file.BIDirectory;
import javax.baja.sys.BComponent;
import javax.baja.sys.BStation;

public class NewApplicationTemplateSource extends NewStationWideTemplateSource {
   public NewApplicationTemplateSource(BStation sourceStation, BIDirectory sourceHomeDir, BIDirectory sourceProtectedHomeDir) {
      super(sourceStation, sourceHomeDir, sourceProtectedHomeDir);
   }

   @Override
   public TemplateType getTemplateType() {
      return TemplateType.APPLICATION;
   }

   @Override
   public List<BComponent> getInstallRoots() {
      return Collections.unmodifiableList(Arrays.asList(ApplicationTemplateUtil.findApplicationRoots((BStation)this.getBase())));
   }

   @Override
   protected String getDefaultStationDirectoryName() {
      return "applicationTemplates";
   }

   @Override
   protected String getFileExtension() {
      return "napl";
   }

   @Override
   protected boolean canStoreFile(BDataFile file) {
      return super.canStoreFile(file) && !file.getFileName().endsWith(".napl");
   }
}
