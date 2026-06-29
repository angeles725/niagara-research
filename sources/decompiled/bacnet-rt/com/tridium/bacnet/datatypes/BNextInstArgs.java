package com.tridium.bacnet.datatypes;

import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BStruct;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.util.BEnumSet;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "objectType",
      type = "int",
      defaultValue = "-1"
   ), @NiagaraProperty(
      name = "siblings",
      type = "BEnumSet",
      defaultValue = "BEnumSet.DEFAULT"
   )})
public class BNextInstArgs extends BStruct {
   public static final Property objectType = newProperty(0, -1, null);
   public static final Property siblings = newProperty(0, BEnumSet.DEFAULT, null);
   public static final Type TYPE = Sys.loadType(BNextInstArgs.class);

   public int getObjectType() {
      return this.getInt(objectType);
   }

   public void setObjectType(int v) {
      this.setInt(objectType, v, null);
   }

   public BEnumSet getSiblings() {
      return (BEnumSet)this.get(siblings);
   }

   public void setSiblings(BEnumSet v) {
      this.set(siblings, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BNextInstArgs() {
   }

   public BNextInstArgs(int otype) {
      this.setObjectType(otype);
   }

   public BNextInstArgs(int otype, int[] sibs) {
      this.setObjectType(otype);
      this.setSiblings(BEnumSet.make(sibs));
   }

   public String toString(Context cx) {
      return this.getObjectType() + "::" + this.getSiblings();
   }
}
