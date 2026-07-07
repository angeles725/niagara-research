package com.tridium.template.api.impl;

import com.tridium.template.api.TemplateType;
import javax.baja.file.BIDirectory;
import javax.baja.sys.BComponent;

public class NewDeviceTemplateSource extends NewInternalTemplateSource {
   public NewDeviceTemplateSource(BComponent sourceComponent, BIDirectory sourceHomeDir, BIDirectory sourceProtectedHomeDir) {
      super(sourceComponent, sourceHomeDir, sourceProtectedHomeDir);
   }

   @Override
   public TemplateType getTemplateType() {
      return TemplateType.DEVICE;
   }
}
