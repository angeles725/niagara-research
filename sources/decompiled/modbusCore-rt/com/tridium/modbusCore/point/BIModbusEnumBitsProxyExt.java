package com.tridium.modbusCore.point;

import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public interface BIModbusEnumBitsProxyExt extends BIModbusNumericBitsProxyExt {
   Type TYPE = Sys.loadType(BIModbusEnumBitsProxyExt.class);
}
