package com.tridium.bacnet.enums;

import javax.baja.nre.annotations.NiagaraEnum;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.annotations.Range;
import javax.baja.sys.BEnum;
import javax.baja.sys.BFrozenEnum;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraEnum(
   range = {@Range("offnormal"), @Range("fault"), @Range("normal"), @Range("all"), @Range("active")},
   defaultValue = "all"
)
public final class BEventStateFilter extends BFrozenEnum {
   public static final int OFFNORMAL = 0;
   public static final int FAULT = 1;
   public static final int NORMAL = 2;
   public static final int ALL = 3;
   public static final int ACTIVE = 4;
   public static final BEventStateFilter offnormal = new BEventStateFilter(0);
   public static final BEventStateFilter fault = new BEventStateFilter(1);
   public static final BEventStateFilter normal = new BEventStateFilter(2);
   public static final BEventStateFilter all = new BEventStateFilter(3);
   public static final BEventStateFilter active = new BEventStateFilter(4);
   public static final BEventStateFilter DEFAULT = all;
   public static final Type TYPE = Sys.loadType(BEventStateFilter.class);

   public static BEventStateFilter make(int ordinal) {
      return (BEventStateFilter)offnormal.getRange().get(ordinal, false);
   }

   public static BEventStateFilter make(String tag) {
      return (BEventStateFilter)offnormal.getRange().get(tag);
   }

   private BEventStateFilter(int ordinal) {
      super(ordinal);
   }

   public Type getType() {
      return TYPE;
   }

   public boolean filter(BEnum eventState) {
      if (eventState == null) {
         return false;
      } else {
         switch (this.getOrdinal()) {
            case 0:
               return eventState.getOrdinal() == 2 || eventState.getOrdinal() == 3 || eventState.getOrdinal() == 4;
            case 1:
               return eventState.getOrdinal() == 1;
            case 2:
               return eventState.getOrdinal() == 0;
            case 3:
               return true;
            case 4:
               return eventState.getOrdinal() != 0;
            default:
               throw new IllegalStateException();
         }
      }
   }
}
