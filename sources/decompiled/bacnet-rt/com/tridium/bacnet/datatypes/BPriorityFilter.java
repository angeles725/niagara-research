package com.tridium.bacnet.datatypes;

import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BFacets;
import javax.baja.sys.BStruct;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "minPriority",
      type = "int",
      defaultValue = "0",
      facets = {@Facet("BFacets.makeInt(0,255)")}
   ), @NiagaraProperty(
      name = "maxPriority",
      type = "int",
      defaultValue = "255",
      facets = {@Facet("BFacets.makeInt(0,255)")}
   )})
public class BPriorityFilter extends BStruct {
   public static final Property minPriority = newProperty(0, 0, BFacets.makeInt(0, 255));
   public static final Property maxPriority = newProperty(0, 255, BFacets.makeInt(0, 255));
   public static final Type TYPE = Sys.loadType(BPriorityFilter.class);

   public int getMinPriority() {
      return this.getInt(minPriority);
   }

   public void setMinPriority(int v) {
      this.setInt(minPriority, v, null);
   }

   public int getMaxPriority() {
      return this.getInt(maxPriority);
   }

   public void setMaxPriority(int v) {
      this.setInt(maxPriority, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BPriorityFilter() {
   }

   public BPriorityFilter(int minPriority, int maxPriority) {
      this.setMinPriority(minPriority);
      this.setMaxPriority(maxPriority);
   }

   public String toString(Context cx) {
      return "min:" + this.getMinPriority() + " max:" + this.getMaxPriority();
   }

   public boolean filter(int priority) {
      return priority >= this.getMinPriority() && priority <= this.getMaxPriority();
   }
}
