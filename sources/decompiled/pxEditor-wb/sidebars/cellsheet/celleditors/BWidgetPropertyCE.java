package com.tridium.px.editor.sidebars.cellsheet.celleditors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BObject;
import javax.baja.sys.BString;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.ui.BWidget;
import javax.baja.workbench.celleditor.BListDropDownCE;

@NiagaraType
public class BWidgetPropertyCE extends BListDropDownCE {
   public static final Type TYPE = Sys.loadType(BWidgetPropertyCE.class);

   public Type getType() {
      return TYPE;
   }

   public BWidgetPropertyCE(BWidget widget, Type allowable) {
      Property[] p = widget.getFrozenPropertiesArray();
      List<String> names = new ArrayList<>();

      for (int i = 0; i < p.length; i++) {
         if (p[i].getType().is(allowable)) {
            names.add(p[i].getName());
         }
      }

      Collections.sort(names);

      for (String name : names) {
         this.getListDropDown().getList().addItem(name);
      }
   }

   protected void doLoadValue(BObject value, Context context) throws Exception {
      this.getListDropDown().getList().setSelectedItem(value.toString());
      super.doLoadValue(value, context);
   }

   protected BObject doSaveValue(BObject value, Context cx) throws Exception {
      return BString.make(this.getListDropDown().getList().getSelectedItem().toString());
   }
}
