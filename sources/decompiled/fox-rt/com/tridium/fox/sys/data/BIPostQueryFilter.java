package com.tridium.fox.sys.data;

import java.util.stream.Stream;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BInterface;
import javax.baja.sys.Context;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.tag.Entity;

@NiagaraType
public interface BIPostQueryFilter extends BInterface {
   Type TYPE = Sys.loadType(BIPostQueryFilter.class);

   Stream<Entity> postQueryFilter(Stream<Entity> var1, Context var2);
}
