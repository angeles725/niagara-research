package com.tridium.lonworks.enums;

import javax.baja.nre.annotations.NiagaraEnum;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.annotations.Range;
import javax.baja.sys.BFrozenEnum;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraEnum(
   range = {@Range("notComp"), @Range("succeeds"), @Range("fails")}
)
public final class BLonCompletionCode extends BFrozenEnum {
   public static final int NOT_COMP = 0;
   public static final int SUCCEEDS = 1;
   public static final int FAILS = 2;
   public static final BLonCompletionCode notComp = new BLonCompletionCode(0);
   public static final BLonCompletionCode succeeds = new BLonCompletionCode(1);
   public static final BLonCompletionCode fails = new BLonCompletionCode(2);
   public static final BLonCompletionCode DEFAULT = notComp;
   public static final Type TYPE = Sys.loadType(BLonCompletionCode.class);

   public static BLonCompletionCode make(int ordinal) {
      return (BLonCompletionCode)notComp.getRange().get(ordinal, false);
   }

   public static BLonCompletionCode make(String tag) {
      return (BLonCompletionCode)notComp.getRange().get(tag);
   }

   private BLonCompletionCode(int ordinal) {
      super(ordinal);
   }

   public Type getType() {
      return TYPE;
   }
}
