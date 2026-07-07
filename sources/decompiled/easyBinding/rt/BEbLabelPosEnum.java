package com.honeywell.easybinding.util;

import javax.baja.nre.annotations.NiagaraEnum;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.annotations.Range;
import javax.baja.sys.BFrozenEnum;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraEnum(
   range = {@Range(
      ordinal = 1,
      value = "left"
   ), @Range(
      ordinal = 2,
      value = "right"
   ), @Range(
      ordinal = 3,
      value = "top"
   ), @Range(
      ordinal = 4,
      value = "bottom"
   )},
   defaultValue = "bottom"
)
public final class BEbLabelPosEnum extends BFrozenEnum {
   public static final int LEFT = 1;
   public static final int RIGHT = 2;
   public static final int TOP = 3;
   public static final int BOTTOM = 4;
   public static final BEbLabelPosEnum left = new BEbLabelPosEnum(1);
   public static final BEbLabelPosEnum right = new BEbLabelPosEnum(2);
   public static final BEbLabelPosEnum top = new BEbLabelPosEnum(3);
   public static final BEbLabelPosEnum bottom = new BEbLabelPosEnum(4);
   public static final BEbLabelPosEnum DEFAULT = bottom;
   public static final Type TYPE = Sys.loadType(BEbLabelPosEnum.class);

   public static BEbLabelPosEnum make(int var0) {
      return (BEbLabelPosEnum)left.getRange().get(var0, false);
   }

   public static BEbLabelPosEnum make(String var0) {
      return (BEbLabelPosEnum)left.getRange().get(var0);
   }

   private BEbLabelPosEnum(int var1) {
      super(var1);
   }

   public Type getType() {
      return TYPE;
   }
}
