package com.tridium.workbench.celleditors;

import javax.baja.nre.annotations.AgentOn;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BBoolean;
import javax.baja.sys.BObject;
import javax.baja.sys.Context;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.ui.BListDropDown;
import javax.baja.workbench.CannotSaveException;
import javax.baja.workbench.celleditor.BListDropDownCE;

@NiagaraType(
   agent = {@AgentOn(
      types = {"baja:Boolean"}
   )}
)
public class BBooleanCE extends BListDropDownCE {
   public static final Type TYPE = Sys.loadType(BBooleanCE.class);

   public Type getType() {
      return TYPE;
   }

   public BBooleanCE() {
      this.getListDropDown().getList().addItem("false");
      this.getListDropDown().getList().addItem("true");
   }

   protected void doLoadValue(BObject value, Context context) throws Exception {
      super.doLoadValue(value, context);
      BBoolean flag = (BBoolean)value;
      this.remove("linkMod");
      this.getListDropDown().setSelectedIndex(flag.getBoolean() ? 1 : 0);
      this.linkTo("linkMod", this.getListDropDown(), BListDropDown.valueModified, dropDownModified);
   }

   protected BObject doSaveValue(BObject value, Context cx) throws CannotSaveException, Exception {
      return this.getListDropDown().getSelectedIndex() == 1 ? BBoolean.TRUE : BBoolean.FALSE;
   }
}
