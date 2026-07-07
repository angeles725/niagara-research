package com.tridium.template.application;

import javax.baja.naming.BOrd;
import javax.baja.naming.BOrdList;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BBoolean;
import javax.baja.sys.BStruct;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "upgrade",
      type = "BBoolean",
      defaultValue = "BBoolean.FALSE"
   ), @NiagaraProperty(
      name = "checkModules",
      type = "BBoolean",
      defaultValue = "BBoolean.FALSE"
   ), @NiagaraProperty(
      name = "fileOrd",
      type = "BOrd",
      defaultValue = "BOrd.DEFAULT"
   ), @NiagaraProperty(
      name = "toBeRemoved",
      type = "BOrdList",
      defaultValue = "BOrdList.make()"
   )})
public class BApplicationInstallSpecs extends BStruct {
   public static final Property upgrade = newProperty(0, ((BBoolean)BBoolean.FALSE.as(BBoolean.class)).getBoolean(), null);
   public static final Property checkModules = newProperty(0, ((BBoolean)BBoolean.FALSE.as(BBoolean.class)).getBoolean(), null);
   public static final Property fileOrd = newProperty(0, BOrd.DEFAULT, null);
   public static final Property toBeRemoved = newProperty(0, BOrdList.make(new BOrd[0]), null);
   public static final Type TYPE = Sys.loadType(BApplicationInstallSpecs.class);

   public boolean getUpgrade() {
      return this.getBoolean(upgrade);
   }

   public void setUpgrade(boolean v) {
      this.setBoolean(upgrade, v, null);
   }

   public boolean getCheckModules() {
      return this.getBoolean(checkModules);
   }

   public void setCheckModules(boolean v) {
      this.setBoolean(checkModules, v, null);
   }

   public BOrd getFileOrd() {
      return (BOrd)this.get(fileOrd);
   }

   public void setFileOrd(BOrd v) {
      this.set(fileOrd, v, null);
   }

   public BOrdList getToBeRemoved() {
      return (BOrdList)this.get(toBeRemoved);
   }

   public void setToBeRemoved(BOrdList v) {
      this.set(toBeRemoved, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public static BApplicationInstallSpecs make() {
      return make(false, false, BOrd.DEFAULT, BOrdList.DEFAULT);
   }

   public static BApplicationInstallSpecs make(boolean upgrade, boolean checkModules, BOrd fileOrd, BOrdList toBeRemoved) {
      BApplicationInstallSpecs specs = new BApplicationInstallSpecs();
      specs.setUpgrade(upgrade);
      specs.setCheckModules(checkModules);
      specs.setFileOrd(fileOrd);
      specs.setToBeRemoved(toBeRemoved);
      return specs;
   }
}
