package com.tridiumemea.extras;

import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BComponent;
import javax.baja.sys.BEnum;
import javax.baja.sys.BFacets;
import javax.baja.sys.BIEnum;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public abstract class BKitPxBuildingBaseEnum extends BComponent implements BIEnum {
   public static final Type TYPE = Sys.loadType(BKitPxBuildingBaseEnum.class);

   public Type getType() {
      return TYPE;
   }

   public final BEnum getEnum() {
      return this.getOut().getEnum();
   }

   public final BFacets getEnumFacets() {
      return this.getOut().getEnum().getEnumFacets();
   }

   public abstract BEnum getOut();

   public abstract void setOut(BEnum var1);
}
