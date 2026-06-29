package com.tridium.modbusCore.point;

import com.tridium.modbusCore.enums.BStatusTypeEnum;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BInterface;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public interface BIModbusBooleanProxyExt extends BInterface {
   Type TYPE = Sys.loadType(BIModbusBooleanProxyExt.class);

   BStatusTypeEnum getStatusType();

   void setStatusType(BStatusTypeEnum var1);
}
