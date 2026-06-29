package com.tridium.modbusCore.point;

import com.tridium.modbusCore.datatypes.BFlexAddress;
import com.tridium.modbusCore.enums.BDataSourceEnum;
import com.tridium.modbusCore.messages.ModbusMessageConst;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BInterface;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public interface BIModbusReadProxyExt extends BInterface, ModbusMessageConst {
   Type TYPE = Sys.loadType(BIModbusReadProxyExt.class);

   BFlexAddress getAbsoluteAddress();

   void setAbsoluteAddress(BFlexAddress var1);

   BDataSourceEnum getDataSource();

   void setDataSource(BDataSourceEnum var1);
}
