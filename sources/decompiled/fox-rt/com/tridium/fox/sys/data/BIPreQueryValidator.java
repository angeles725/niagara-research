package com.tridium.fox.sys.data;

import com.tridium.fox.sys.NiagaraStation;
import javax.baja.naming.BOrdList;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BInterface;
import javax.baja.sys.Context;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public interface BIPreQueryValidator extends BInterface {
   Type TYPE = Sys.loadType(BIPreQueryValidator.class);

   void validateRemoteQuery(NiagaraStation var1, BOrdList var2, int var3, int var4, Context var5);
}
