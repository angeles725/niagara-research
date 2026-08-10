package com.tridium.workbench.tools;

import com.tridium.workbench.auth.AuthUtil;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.ui.CommandArtifact;
import javax.baja.workbench.BWbShell;
import javax.baja.workbench.tool.BWbTool;

@NiagaraType
public class BManageCredentialsTool extends BWbTool {
   public static final Type TYPE = Sys.loadType(BManageCredentialsTool.class);

   public Type getType() {
      return TYPE;
   }

   public CommandArtifact invoke(BWbShell shell) throws Exception {
      AuthUtil.manageCredentials(shell, TYPE.getDisplayName(null));
      return null;
   }
}
