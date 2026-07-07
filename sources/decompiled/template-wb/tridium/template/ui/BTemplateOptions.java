package com.tridium.template.ui;

import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.ui.options.BUserOptions;

@NiagaraType
@NiagaraProperty(
   name = "useMinorVersionOnDeployment",
   type = "boolean",
   defaultValue = "true"
)
public final class BTemplateOptions extends BUserOptions {
   public static final Property useMinorVersionOnDeployment = newProperty(0, true, null);
   public static final Type TYPE = Sys.loadType(BTemplateOptions.class);
   private static BTemplateOptions options;

   public boolean getUseMinorVersionOnDeployment() {
      return this.getBoolean(useMinorVersionOnDeployment);
   }

   public void setUseMinorVersionOnDeployment(boolean v) {
      this.setBoolean(useMinorVersionOnDeployment, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public static BTemplateOptions get() {
      if (options == null) {
         options = (BTemplateOptions)load(TYPE);
      }

      return options;
   }
}
