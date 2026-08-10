package com.tridium.driver.file.ui;

import com.tridium.driver.file.BFileDevice;
import javax.baja.driver.BDevice;
import javax.baja.driver.ui.device.BDeviceManager;
import javax.baja.driver.ui.device.DeviceExtsColumn;
import javax.baja.driver.ui.device.DeviceModel;
import javax.baja.nre.annotations.AgentOn;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.workbench.mgr.MgrColumn;
import javax.baja.workbench.mgr.MgrModel;
import javax.baja.workbench.mgr.MgrColumn.Name;
import javax.baja.workbench.mgr.MgrColumn.Path;
import javax.baja.workbench.mgr.MgrColumn.Prop;

@NiagaraType(
   agent = {@AgentOn(
      types = {"driver:FileNetwork", "driver:FileDeviceFolder"},
      requiredPermissions = "r"
   )}
)
public class BFileDeviceManager extends BDeviceManager {
   public static final Type TYPE = Sys.loadType(BFileDeviceManager.class);

   @Override
   public Type getType() {
      return TYPE;
   }

   @Override
   protected MgrModel makeModel() {
      return new BFileDeviceManager.FileDeviceModel(this);
   }

   public class FileDeviceModel extends DeviceModel {
      FileDeviceModel(BFileDeviceManager mgr) {
         super(mgr);
      }

      @Override
      protected MgrColumn[] makeColumns() {
         return new MgrColumn[]{
            new Path(2),
            new Name(),
            new javax.baja.workbench.mgr.MgrColumn.Type(),
            new DeviceExtsColumn(new BFileDevice()),
            new Prop(BFileDevice.baseOrd, 1),
            new Prop(BDevice.status),
            new Prop(BDevice.enabled, 3),
            new Prop(BDevice.health),
            new Prop(BDevice.faultCause)
         };
      }
   }
}
