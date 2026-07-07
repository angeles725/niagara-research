package com.honeywell.easybinding.tool;

import com.honeywell.easybinding.tool.wizard.EasyPaletteWizardMain;
import com.honeywell.easybinding.util.EbLicenseUtil;
import com.honeywell.easybinding.util.KitpxUtils;
import javax.baja.license.Feature;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.ui.CommandArtifact;
import javax.baja.workbench.BWbShell;
import javax.baja.workbench.tool.BWbTool;

@NiagaraType
public class BEasyPaletteBuilder extends BWbTool {
   public static final Type TYPE = Sys.loadType(BEasyPaletteBuilder.class);

   public Type getType() {
      return TYPE;
   }

   public CommandArtifact invoke(BWbShell var1) throws Exception {
      new EasyPaletteWizardMain(var1).open();
      return null;
   }

   public Feature getLicenseFeature() {
      Feature var1 = EbLicenseUtil.checkEasyBindingFeature();
      KitpxUtils.setLicense(var1);
      return var1;
   }
}
