package com.tridium.template;

import java.util.Optional;
import javax.baja.naming.BOrd;
import javax.baja.naming.OrdTarget;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.security.BPassword;
import javax.baja.sys.BComplex;
import javax.baja.sys.BComponent;
import javax.baja.sys.BStruct;
import javax.baja.sys.BValue;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "pswOrd",
      type = "BOrd",
      defaultValue = "BOrd.NULL"
   ), @NiagaraProperty(
      name = "pswParentSlot",
      type = "String",
      defaultValue = ""
   ), @NiagaraProperty(
      name = "pswSlot",
      type = "String",
      defaultValue = ""
   ), @NiagaraProperty(
      name = "isDynamic",
      type = "boolean",
      defaultValue = "false",
      flags = 4
   )})
public class BPasswordBinding extends BStruct {
   public static final Property pswOrd = newProperty(0, BOrd.NULL, null);
   public static final Property pswParentSlot = newProperty(0, "", null);
   public static final Property pswSlot = newProperty(0, "", null);
   public static final Property isDynamic = newProperty(4, false, null);
   public static final Type TYPE = Sys.loadType(BPasswordBinding.class);

   public BOrd getPswOrd() {
      return (BOrd)this.get(pswOrd);
   }

   public void setPswOrd(BOrd v) {
      this.set(pswOrd, v, null);
   }

   public String getPswParentSlot() {
      return this.getString(pswParentSlot);
   }

   public void setPswParentSlot(String v) {
      this.setString(pswParentSlot, v, null);
   }

   public String getPswSlot() {
      return this.getString(pswSlot);
   }

   public void setPswSlot(String v) {
      this.setString(pswSlot, v, null);
   }

   public boolean getIsDynamic() {
      return this.getBoolean(isDynamic);
   }

   public void setIsDynamic(boolean v) {
      this.setBoolean(isDynamic, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BPasswordBinding() {
   }

   public BPasswordBinding(BOrd target, String pswSlot, boolean isDynamic) {
      this.setPswOrd(target);
      this.setPswSlot(pswSlot);
      this.setIsDynamic(isDynamic);
   }

   public BPasswordBinding(BOrd target, String pswParentSlot, String pswSlot, boolean isDynamic) {
      this.setPswOrd(target);
      this.setPswParentSlot(pswParentSlot);
      this.setPswSlot(pswSlot);
      this.setIsDynamic(isDynamic);
   }

   public Optional<BComponent> getTarget() {
      try {
         OrdTarget ordTarget = this.getPswOrd().resolve(this.getParentComponent());
         return Optional.of(ordTarget.get().asComponent());
      } catch (Exception var2) {
         return Optional.empty();
      }
   }

   public boolean isPasswordDefault() {
      Optional<BComponent> passwordTarget = this.getTarget();
      if (!passwordTarget.isPresent()) {
         return false;
      } else {
         BComponent pswTarget = passwordTarget.get();
         BComplex pswParent = pswTarget;
         pswTarget.lease();
         String pswParentSlot = this.getPswParentSlot();
         if (pswParentSlot != null && !pswParentSlot.isEmpty()) {
            BValue v = pswTarget.get(pswParentSlot);
            if (!(v instanceof BComplex)) {
               return false;
            }

            pswParent = v.asComplex();
         }

         BValue bValue = pswParent.get(this.getPswSlot());
         return bValue instanceof BPassword && ((BPassword)bValue).isDefault();
      }
   }
}
