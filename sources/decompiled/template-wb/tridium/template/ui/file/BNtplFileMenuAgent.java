package com.tridium.template.ui.file;

import com.tridium.workbench.nav.BFileMenuAgent;
import javax.baja.nre.annotations.AgentOn;
import javax.baja.nre.annotations.NiagaraSingleton;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BObject;
import javax.baja.sys.Context;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.ui.BMenu;
import javax.baja.ui.BSeparator;
import javax.baja.ui.BSubMenuItem;
import javax.baja.ui.BWidget;
import javax.baja.workbench.nav.menu.BNavMenuAgent;
import javax.baja.workbench.nav.menu.NavMenuUtil;

@NiagaraType(
   agent = {@AgentOn(
      types = {"template:WbDeployableNtplFile"}
   )}
)
@NiagaraSingleton
public class BNtplFileMenuAgent extends BNavMenuAgent {
   public static final BNtplFileMenuAgent INSTANCE = new BNtplFileMenuAgent();
   public static final Type TYPE = Sys.loadType(BNtplFileMenuAgent.class);

   public Type getType() {
      return TYPE;
   }

   protected BMenu doMakeMenu(BWidget owner, BObject target, Context cx) {
      BWbDeployableNtplFile ntplFile = (BWbDeployableNtplFile)target;
      BMenu menu = new BMenu();
      BMenu fileViews = NavMenuUtil.makeViewsMenu(owner, target, ntplFile.getAbsoluteOrd());
      menu.add(null, new BSubMenuItem(fileViews));
      menu.add(null, new BSeparator());
      BFileMenuAgent.addEditCommands(menu, owner, ntplFile);
      menu.add(null, new BSeparator());
      menu.addItem("exportConfigs", new ExportConfigsCommand(owner));
      return menu;
   }
}
