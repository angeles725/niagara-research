package com.tridium.template.api.impl;

import com.tridium.template.BTemplateConfig;
import com.tridium.template.api.TemplateType;
import java.util.Objects;
import javax.baja.sys.BStation;

public class InstalledApplicationTemplateSource extends DeployedTemplateSource {
   public static InstalledApplicationTemplateSource make(BStation station) {
      Objects.requireNonNull(station);
      BTemplateConfig config = BTemplateConfig.getConfigForApplication(station);
      return config != null ? new InstalledApplicationTemplateSource(station, config) : null;
   }

   public InstalledApplicationTemplateSource(BStation station) {
      super(station, BTemplateConfig.getConfigForApplication(station));
   }

   private InstalledApplicationTemplateSource(BStation station, BTemplateConfig config) {
      super(station, config);
   }

   @Override
   public TemplateType getTemplateType() {
      return TemplateType.APPLICATION;
   }

   @Override
   protected String getFileExtension() {
      return "napl";
   }
}
