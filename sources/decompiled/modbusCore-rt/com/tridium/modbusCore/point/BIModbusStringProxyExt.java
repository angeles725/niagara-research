package com.tridium.modbusCore.point;

import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BInterface;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public interface BIModbusStringProxyExt extends BInterface {
   Type TYPE = Sys.loadType(BIModbusStringProxyExt.class);

   int getNumberRegisters();

   void setNumberRegisters(int var1);
}
