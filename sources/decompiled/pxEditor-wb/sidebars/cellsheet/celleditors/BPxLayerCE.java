package com.tridium.px.editor.sidebars.cellsheet.celleditors;

import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BObject;
import javax.baja.sys.BString;
import javax.baja.sys.Context;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.ui.list.BList;
import javax.baja.ui.px.BLayerTag;
import javax.baja.ui.px.PxLayer;
import javax.baja.workbench.celleditor.BListDropDownCE;

@NiagaraType
public class BPxLayerCE extends BListDropDownCE {
   public static final Type TYPE = Sys.loadType(BPxLayerCE.class);
   private static final String LAYER = "layer";

   public Type getType() {
      return TYPE;
   }

   public BPxLayerCE(PxLayer[] layers) {
      this.setPropertyName("layer");
      BList list = this.getListDropDown().getList();
      list.addItem("");

      for (int i = 0; i < layers.length; i++) {
         list.addItem(layers[i].getName());
      }
   }

   protected void doLoadValue(BObject value, Context context) throws Exception {
      if (value.isNull()) {
         this.getListDropDown().getList().setSelectedItem("");
      } else {
         BLayerTag tag = (BLayerTag)value;
         this.getListDropDown().getList().setSelectedItem(tag.getLayerName());
      }

      super.doLoadValue(value, context);
   }

   protected BObject doSaveValue(BObject value, Context cx) throws Exception {
      return BString.make(this.getListDropDown().getList().getSelectedItem().toString());
   }
}
