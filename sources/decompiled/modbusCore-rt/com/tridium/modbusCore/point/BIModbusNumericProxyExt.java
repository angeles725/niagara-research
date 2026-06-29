package com.tridium.modbusCore.point;

import com.tridium.modbusCore.enums.BDataTypeEnum;
import com.tridium.modbusCore.enums.BRegisterTypeEnum;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BInterface;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public interface BIModbusNumericProxyExt extends BInterface {
   Type TYPE = Sys.loadType(BIModbusNumericProxyExt.class);

   BRegisterTypeEnum getRegType();

   void setRegType(BRegisterTypeEnum var1);

   BDataTypeEnum getDataType();

   void setDataType(BDataTypeEnum var1);
}
