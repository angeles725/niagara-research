package com.tridium.template.api.impl;

import com.tridium.template.api.TemplateType;
import java.util.Collections;
import java.util.List;
import javax.baja.file.BIDirectory;
import javax.baja.sys.BComponent;
import javax.baja.sys.BStation;

public class NewStationTemplateSource extends NewStationWideTemplateSource {
   public NewStationTemplateSource(BStation sourceStation, BIDirectory sourceHomeDir, BIDirectory sourceProtectedHomeDir) {
      super(sourceStation, sourceHomeDir, sourceProtectedHomeDir);
   }

   @Override
   protected List<BComponent> getInstallRoots() {
      return Collections.emptyList();
   }

   @Override
   public TemplateType getTemplateType() {
      return TemplateType.STATION;
   }

   @Override
   protected String getDefaultStationDirectoryName() {
      return "stationTemplates";
   }
}
