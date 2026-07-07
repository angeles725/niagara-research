package com.tridium.template;

import java.util.Optional;
import javax.baja.naming.BOrd;
import javax.baja.naming.OrdTarget;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BComplex;
import javax.baja.sys.BStruct;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "targetOrd",
      type = "BOrd",
      defaultValue = "BOrd.NULL"
   ), @NiagaraProperty(
      name = "sourceSlot",
      type = "String",
      defaultValue = ""
   ), @NiagaraProperty(
      name = "targetSlot",
      type = "String",
      defaultValue = ""
   ), @NiagaraProperty(
      name = "userTip",
      type = "String",
      defaultValue = ""
   )})
public class BConfigBinding extends BStruct {
   public static final Property targetOrd = newProperty(0, BOrd.NULL, null);
   public static final Property sourceSlot = newProperty(0, "", null);
   public static final Property targetSlot = newProperty(0, "", null);
   public static final Property userTip = newProperty(0, "", null);
   public static final Type TYPE = Sys.loadType(BConfigBinding.class);

   public BOrd getTargetOrd() {
      return (BOrd)this.get(targetOrd);
   }

   public void setTargetOrd(BOrd v) {
      this.set(targetOrd, v, null);
   }

   public String getSourceSlot() {
      return this.getString(sourceSlot);
   }

   public void setSourceSlot(String v) {
      this.setString(sourceSlot, v, null);
   }

   public String getTargetSlot() {
      return this.getString(targetSlot);
   }

   public void setTargetSlot(String v) {
      this.setString(targetSlot, v, null);
   }

   public String getUserTip() {
      return this.getString(userTip);
   }

   public void setUserTip(String v) {
      this.setString(userTip, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BConfigBinding() {
   }

   public BConfigBinding(BOrd target, String sourceSlot, String targetSlot) {
      this.setTargetOrd(target);
      this.setSourceSlot(sourceSlot);
      this.setTargetSlot(targetSlot);
      this.setUserTip("");
   }

   public BConfigBinding(BOrd target, String sourceSlot, String targetSlot, String userTip) {
      this.setTargetOrd(target);
      this.setSourceSlot(sourceSlot);
      this.setTargetSlot(targetSlot);
      this.setUserTip(userTip);
   }

   public Optional<BComplex> getTarget() {
      try {
         OrdTarget ordTarget = this.getTargetOrd().resolve(this.getParentComponent());
         return Optional.of(ordTarget.get().asComplex());
      } catch (Exception var2) {
         return Optional.empty();
      }
   }
}
