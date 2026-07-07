package com.tridium.px.editor.sidebars.cellsheet.commands;

import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.ui.BBinding;
import javax.baja.ui.pane.BEdgePane;
import javax.baja.util.BConverter;

@NiagaraType
public abstract class BConverterConfig extends BEdgePane {
   public static final Type TYPE = Sys.loadType(BConverterConfig.class);

   public Type getType() {
      return TYPE;
   }

   abstract BBinding binding();

   abstract BConverter converter();
}
