package com.tridium.workbench.tools;

import java.lang.reflect.Method;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BModule;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.ui.BWidget;
import javax.baja.ui.BWidgetShell;
import javax.baja.ui.CommandArtifact;
import javax.baja.workbench.BWbShell;
import javax.baja.workbench.tool.BWbTool;

@NiagaraType
public class BRequestLicenseTool extends BWbTool {
   public static final Type TYPE = Sys.loadType(BRequestLicenseTool.class);

   public Type getType() {
      return TYPE;
   }

   public CommandArtifact invoke(BWbShell shell) throws Exception {
      requestLicense(shell, null);
      return null;
   }

   public static void requestLicense(BWidget owner, String hostId) throws Exception {
      BModule module = Sys.loadModule("portalApi");
      Class<?> cls = module.loadClass("com.tridium.portal.wb.LicenseProcedure");
      Method m = cls.getMethod("requestLicense", BWidgetShell.class, String.class);
      m.invoke(null, owner, hostId);
   }
}
