package com.tridium.opcUaClient.point;

import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BIObject;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public interface BIOpcUaObject extends BIObject {
   Type TYPE = Sys.loadType(BIOpcUaObject.class);

   String getUaNodeId();

   void setUaNodeId(String var1);
}
