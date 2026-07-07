package com.tridium.px.editor.sidebars.cellsheet.celleditors;

import javax.baja.nre.annotations.AgentOn;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BFacets;
import javax.baja.sys.BInteger;
import javax.baja.sys.BObject;
import javax.baja.sys.Context;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.workbench.celleditor.BButtonCE;
import javax.baja.workbench.fieldeditor.BWbFieldEditor;

@NiagaraType(
   agent = {@AgentOn(
      types = {"gx:Size"}
   )}
)
public class BSizeCE extends BButtonCE {
   public static final Type TYPE = Sys.loadType(BSizeCE.class);

   public Type getType() {
      return TYPE;
   }

   protected BObject dialog() throws Exception {
      Context cx = this.getCurrentContext();
      BFacets facets = cx == null ? null : cx.getFacets();
      if (facets == null) {
         facets = BFacets.make("min", BInteger.make(0));
      } else {
         facets = BFacets.make(facets, "min", BInteger.make(0));
      }

      facets = BFacets.make(facets, "max", BInteger.make(10000));
      return BWbFieldEditor.dialog(this, this.getPropertyName(), this.value, facets);
   }
}
