package com.honeywell.easybinding.bindings;

import com.tridium.kitpx.enums.BStatusEffect;
import com.tridiumx.ps.util.BSecureBoundLabelBinding;
import javax.baja.nre.annotations.AgentOn;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.annotations.AgentOn.Preference;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType(
   agent = {@AgentOn(
      types = {"workbench:WebWidget"},
      defaultAgent = Preference.PREFERRED,
      requiredPermissions = "rw"
   )}
)
public class BEasyBaseBinding extends BSecureBoundLabelBinding {
   public static final Type TYPE = Sys.loadType(BEasyBaseBinding.class);

   public Type getType() {
      return TYPE;
   }

   public BEasyBaseBinding() {
      this.setFlags(degradeBehavior, 4);
      this.setFlags(summary, 4);
      this.setFlags(hyperlink, 4);
      this.setFlags(popupEnabled, 4);
      this.setStatusEffect(BStatusEffect.none);
   }
}
