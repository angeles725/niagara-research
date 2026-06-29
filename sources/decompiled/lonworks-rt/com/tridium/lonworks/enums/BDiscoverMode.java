package com.tridium.lonworks.enums;

import javax.baja.nre.annotations.NiagaraEnum;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.annotations.Range;
import javax.baja.sys.BFrozenEnum;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraEnum(
   range = {@Range("noOptions"), @Range("absolute"), @Range("readOnlyRelative"), @Range("configRelative")}
)
public final class BDiscoverMode extends BFrozenEnum {
   public static final int NO_OPTIONS = 0;
   public static final int ABSOLUTE = 1;
   public static final int READ_ONLY_RELATIVE = 2;
   public static final int CONFIG_RELATIVE = 3;
   public static final BDiscoverMode noOptions = new BDiscoverMode(0);
   public static final BDiscoverMode absolute = new BDiscoverMode(1);
   public static final BDiscoverMode readOnlyRelative = new BDiscoverMode(2);
   public static final BDiscoverMode configRelative = new BDiscoverMode(3);
   public static final BDiscoverMode DEFAULT = noOptions;
   public static final Type TYPE = Sys.loadType(BDiscoverMode.class);

   public static BDiscoverMode make(int ordinal) {
      return (BDiscoverMode)noOptions.getRange().get(ordinal, false);
   }

   public static BDiscoverMode make(String tag) {
      return (BDiscoverMode)noOptions.getRange().get(tag);
   }

   private BDiscoverMode(int ordinal) {
      super(ordinal);
   }

   public Type getType() {
      return TYPE;
   }
}
