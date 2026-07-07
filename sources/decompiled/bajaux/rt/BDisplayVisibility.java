package com.tridium.ux;

import javax.baja.nre.annotations.NiagaraEnum;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.annotations.Range;
import javax.baja.sys.BFrozenEnum;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraEnum(
   range = {@Range(
      value = "responsive",
      ordinal = 0
   ), @Range(
      value = "show",
      ordinal = 1
   ), @Range(
      value = "hide",
      ordinal = 2
   )},
   defaultValue = "responsive"
)
public final class BDisplayVisibility extends BFrozenEnum {
   public static final int RESPONSIVE = 0;
   public static final int SHOW = 1;
   public static final int HIDE = 2;
   public static final BDisplayVisibility responsive = new BDisplayVisibility(0);
   public static final BDisplayVisibility show = new BDisplayVisibility(1);
   public static final BDisplayVisibility hide = new BDisplayVisibility(2);
   public static final BDisplayVisibility DEFAULT = responsive;
   public static final Type TYPE = Sys.loadType(BDisplayVisibility.class);

   public static BDisplayVisibility make(int ordinal) {
      return (BDisplayVisibility)responsive.getRange().get(ordinal, false);
   }

   public static BDisplayVisibility make(String tag) {
      return (BDisplayVisibility)responsive.getRange().get(tag);
   }

   private BDisplayVisibility(int ordinal) {
      super(ordinal);
   }

   public Type getType() {
      return TYPE;
   }
}
