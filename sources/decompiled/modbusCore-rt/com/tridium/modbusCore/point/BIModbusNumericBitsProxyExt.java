package com.tridium.modbusCore.point;

import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public interface BIModbusNumericBitsProxyExt extends BIModbusNumericProxyExt {
   Type TYPE = Sys.loadType(BIModbusNumericBitsProxyExt.class);

   int getBeginningBit();

   void setBeginningBit(int var1);

   int getNumberOfBits();

   void setNumberOfBits(int var1);
}
