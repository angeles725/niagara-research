package com.tridium.fox.sys;

import com.tridium.space.BGateway;
import javax.baja.agent.BIAgent;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.space.BSpace;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public interface BIFoxGatewayProxyFactory extends BIAgent {
   Type TYPE = Sys.loadType(BIFoxGatewayProxyFactory.class);

   BSpace makeFoxGatewayProxySpace(BGateway var1);
}
