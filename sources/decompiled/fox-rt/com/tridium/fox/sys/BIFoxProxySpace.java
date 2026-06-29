package com.tridium.fox.sys;

import javax.baja.nav.BINavNode;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public interface BIFoxProxySpace extends BINavNode {
   Type TYPE = Sys.loadType(BIFoxProxySpace.class);

   void init(BFoxSession var1) throws Exception;

   void cleanup(BFoxSession var1) throws Exception;
}
