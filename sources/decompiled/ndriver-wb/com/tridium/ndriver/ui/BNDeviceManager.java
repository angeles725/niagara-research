package com.tridium.ndriver.ui.device;

import com.tridium.ndriver.BNNetwork;
import com.tridium.ndriver.ui.NMgrLearn;
import com.tridium.ndriver.ui.NMgrUtil;
import javax.baja.driver.ui.device.BDeviceManager;
import javax.baja.nre.annotations.AgentOn;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.workbench.mgr.MgrController;
import javax.baja.workbench.mgr.MgrLearn;
import javax.baja.workbench.mgr.MgrModel;
import javax.baja.workbench.mgr.MgrState;

@NiagaraType(
   agent = {@AgentOn(
      types = {"ndriver:NNetwork", "ndriver:NDeviceFolder"}
   )}
)
public class BNDeviceManager extends BDeviceManager {
   public static final Type TYPE = Sys.loadType(BNDeviceManager.class);

   public Type getType() {
      return TYPE;
   }

   protected MgrModel makeModel() {
      return new NDeviceModel(this);
   }

   protected MgrLearn makeLearn() {
      Type discoveryLeafType = NMgrUtil.getDiscoveryLeafType(this);
      return discoveryLeafType == null ? null : new NMgrLearn(this);
   }

   protected MgrController makeController() {
      return new NDeviceController(this);
   }

   protected MgrState makeState() {
      return new NDeviceState();
   }

   public BNNetwork getNNetwork() {
      return NMgrUtil.findNNetwork(this);
   }
}
