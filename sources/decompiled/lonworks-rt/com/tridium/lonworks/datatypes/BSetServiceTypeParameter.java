package com.tridium.lonworks.datatypes;

import javax.baja.lonworks.enums.BLonLinkType;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BStruct;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "linkType",
      type = "BLonLinkType",
      defaultValue = "BLonLinkType.unknown"
   ), @NiagaraProperty(
      name = "selector",
      type = "String",
      defaultValue = ""
   ), @NiagaraProperty(
      name = "priority",
      type = "boolean",
      defaultValue = "false"
   )})
public class BSetServiceTypeParameter extends BStruct {
   public static final Property linkType = newProperty(0, BLonLinkType.unknown, null);
   public static final Property selector = newProperty(0, "", null);
   public static final Property priority = newProperty(0, false, null);
   public static final Type TYPE = Sys.loadType(BSetServiceTypeParameter.class);

   public BLonLinkType getLinkType() {
      return (BLonLinkType)this.get(linkType);
   }

   public void setLinkType(BLonLinkType v) {
      this.set(linkType, v, null);
   }

   public String getSelector() {
      return this.getString(selector);
   }

   public void setSelector(String v) {
      this.setString(selector, v, null);
   }

   public boolean getPriority() {
      return this.getBoolean(priority);
   }

   public void setPriority(boolean v) {
      this.setBoolean(priority, v, null);
   }

   public Type getType() {
      return TYPE;
   }
}
