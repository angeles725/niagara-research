package com.tridium.modbusCore.client.point;

import com.tridium.modbusCore.client.BModbusClientDevice;
import javax.baja.driver.point.BPointDeviceExt;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BComponent;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public class BModbusClientPointDeviceExt extends BPointDeviceExt {
   public static final Type TYPE = Sys.loadType(BModbusClientPointDeviceExt.class);

   public Type getType() {
      return TYPE;
   }

   public Type getDeviceType() {
      return BModbusClientDevice.TYPE;
   }

   public Type getProxyExtType() {
      return BModbusClientProxyExt.TYPE;
   }

   public Type getPointFolderType() {
      return BModbusClientPointFolder.TYPE;
   }

   public boolean isParentLegal(BComponent parent) {
      return parent instanceof BModbusClientDevice;
   }
}
