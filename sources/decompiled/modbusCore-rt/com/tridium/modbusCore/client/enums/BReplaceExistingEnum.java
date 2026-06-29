package com.tridium.modbusCore.client.enums;

import javax.baja.nre.annotations.NiagaraEnum;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.annotations.Range;
import javax.baja.sys.BFrozenEnum;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraEnum(
   range = {@Range("replaceExistingEntries"), @Range("appendToExistingEntries")}
)
public final class BReplaceExistingEnum extends BFrozenEnum {
   public static final int REPLACE_EXISTING_ENTRIES = 0;
   public static final int APPEND_TO_EXISTING_ENTRIES = 1;
   public static final BReplaceExistingEnum replaceExistingEntries = new BReplaceExistingEnum(0);
   public static final BReplaceExistingEnum appendToExistingEntries = new BReplaceExistingEnum(1);
   public static final BReplaceExistingEnum DEFAULT = replaceExistingEntries;
   public static final Type TYPE = Sys.loadType(BReplaceExistingEnum.class);

   public static BReplaceExistingEnum make(int ordinal) {
      return (BReplaceExistingEnum)replaceExistingEntries.getRange().get(ordinal, false);
   }

   public static BReplaceExistingEnum make(String tag) {
      return (BReplaceExistingEnum)replaceExistingEntries.getRange().get(tag);
   }

   private BReplaceExistingEnum(int ordinal) {
      super(ordinal);
   }

   public Type getType() {
      return TYPE;
   }
}
