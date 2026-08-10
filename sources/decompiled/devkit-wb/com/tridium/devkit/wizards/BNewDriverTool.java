package com.tridium.devkit.wizards;

import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.ui.CommandArtifact;
import javax.baja.workbench.BWbShell;
import javax.baja.workbench.tool.BWbTool;

@NiagaraType
public class BNewDriverTool extends BWbTool {
   public static final Type TYPE = Sys.loadType(BNewDriverTool.class);

   public Type getType() {
      return TYPE;
   }

   public CommandArtifact invoke(BWbShell shell) throws Exception {
      NewDriverWizard2.kickIt(shell);
      return null;
   }
}
