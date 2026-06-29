package com.tridium.opcUaServer.point;

import javax.baja.driver.point.BIPointFolder;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public interface BIOpcUaServerPointFolder extends BIPointFolder {
   Type TYPE = Sys.loadType(BIOpcUaServerPointFolder.class);

   String getUaNodeId();
}
