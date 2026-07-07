package com.tridium.template;

import com.tridium.install.BVersion;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BStruct;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "modulePartName",
      type = "String",
      defaultValue = ""
   ), @NiagaraProperty(
      name = "moduleVersion",
      type = "BVersion",
      defaultValue = "new BVersion()"
   )})
public class BTemplateValues extends BStruct {
   public static final Property modulePartName = newProperty(0, "", null);
   public static final Property moduleVersion = newProperty(0, new BVersion(), null);
   public static final Type TYPE = Sys.loadType(BTemplateValues.class);

   public String getModulePartName() {
      return this.getString(modulePartName);
   }

   public void setModulePartName(String v) {
      this.setString(modulePartName, v, null);
   }

   public BVersion getModuleVersion() {
      return (BVersion)this.get(moduleVersion);
   }

   public void setModuleVersion(BVersion v) {
      this.set(moduleVersion, v, null);
   }

   public Type getType() {
      return TYPE;
   }
}
