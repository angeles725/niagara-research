package com.tridium.modbusCore.point;

import com.tridium.modbusCore.enums.BRegisterTypeEnum;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BInterface;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public interface BIModbusRegisterBitProxyExt extends BInterface {
   Type TYPE = Sys.loadType(BIModbusRegisterBitProxyExt.class);

   BRegisterTypeEnum getRegType();

   void setRegType(BRegisterTypeEnum var1);

   int getBitNumber();

   void setBitNumber(int var1);
}
