package com.tridium.px.editor.sidebars.cellsheet.celleditors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Action;
import javax.baja.sys.BObject;
import javax.baja.sys.BString;
import javax.baja.sys.Context;
import javax.baja.sys.Sys;
import javax.baja.sys.Topic;
import javax.baja.sys.Type;
import javax.baja.ui.BWidget;
import javax.baja.workbench.celleditor.BListDropDownCE;

@NiagaraType
public class BWidgetEventCE extends BListDropDownCE {
   public static final Type TYPE = Sys.loadType(BWidgetEventCE.class);

   public Type getType() {
      return TYPE;
   }

   public BWidgetEventCE(BWidget widget, String... additionalItems) {
      Action[] a = widget.getActionsArray();
      Topic[] t = widget.getTopicsArray();
      List<String> names = new ArrayList<>();

      for (int i = 0; i < a.length; i++) {
         if (a[i].isFrozen()) {
            names.add(a[i].getName());
         }
      }

      for (int ix = 0; ix < t.length; ix++) {
         if (t[ix].isFrozen()) {
            names.add(t[ix].getName());
         }
      }

      Collections.addAll(names, additionalItems);
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
