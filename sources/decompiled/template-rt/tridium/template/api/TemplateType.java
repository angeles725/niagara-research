package com.tridium.template.api;

import java.util.Locale;

public enum TemplateType {
   UNSPECIFIED,
   COMPONENT,
   DEVICE,
   STATION,
   APPLICATION;

   public String friendlyName() {
      String name = this.name();
      return name.substring(0, 1).toUpperCase(Locale.ENGLISH) + name.substring(1).toLowerCase(Locale.ENGLISH);
   }
}
