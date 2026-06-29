package com.tridium.bacnet.datatypes;

import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BComponent;
import javax.baja.sys.BFacets;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperty(
   name = "value",
   type = "String",
   defaultValue = "",
   flags = 5,
   facets = {@Facet(
      name = "BFacets.MULTI_LINE",
      value = "true"
   )}
)
public class BChangeDeviceIdConfig extends BComponent {
   public static final Property value = newProperty(5, "", BFacets.make("multiLine", true));
   public static final Type TYPE = Sys.loadType(BChangeDeviceIdConfig.class);

   public String getValue() {
      return this.getString(value);
   }

   public void setValue(String v) {
      this.setString(value, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public void addChange(BBacnetObjectIdentifier currentId, BBacnetObjectIdentifier newId) {
      this.add("currentId?", currentId, 1);
      this.add("changeTo?", newId, 1);
      String v = this.getValue();
      if (v.length() > 0) {
         v = v + "; ";
      }

      this.setValue(v + currentId + "->" + newId);
   }

   public String toString(Context cx) {
      return "Change DeviceIDs:" + this.getValue();
   }
}
