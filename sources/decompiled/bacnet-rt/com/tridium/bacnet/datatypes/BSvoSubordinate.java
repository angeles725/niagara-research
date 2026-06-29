package com.tridium.bacnet.datatypes;

import javax.baja.bacnet.BacnetConst;
import javax.baja.bacnet.datatypes.BBacnetDeviceObjectReference;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BStruct;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "reference",
      type = "BBacnetDeviceObjectReference",
      defaultValue = "new BBacnetDeviceObjectReference()"
   ), @NiagaraProperty(
      name = "annotation",
      type = "String",
      defaultValue = ""
   )})
public class BSvoSubordinate extends BStruct {
   public static final Property reference = newProperty(0, new BBacnetDeviceObjectReference(), null);
   public static final Property annotation = newProperty(0, "", null);
   public static final Type TYPE = Sys.loadType(BSvoSubordinate.class);

   public BBacnetDeviceObjectReference getReference() {
      return (BBacnetDeviceObjectReference)this.get(reference);
   }

   public void setReference(BBacnetDeviceObjectReference v) {
      this.set(reference, v, null);
   }

   public String getAnnotation() {
      return this.getString(annotation);
   }

   public void setAnnotation(String v) {
      this.setString(annotation, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BSvoSubordinate() {
   }

   public BSvoSubordinate(BBacnetDeviceObjectReference ref, String ann) {
      this.setReference((BBacnetDeviceObjectReference)ref.newCopy());
      this.setAnnotation(ann);
   }

   public String toString(Context cx) {
      StringBuilder s = new StringBuilder();
      s.append(this.getReference().toString(cx)).append(this.sep(cx)).append(this.getAnnotation());
      return s.toString();
   }

   private String sep(Context cx) {
      return cx == BacnetConst.nameContext ? "_" : "::";
   }
}
