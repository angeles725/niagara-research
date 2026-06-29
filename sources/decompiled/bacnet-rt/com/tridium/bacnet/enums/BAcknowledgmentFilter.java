package com.tridium.bacnet.enums;

import javax.baja.bacnet.datatypes.BBacnetBitString;
import javax.baja.nre.annotations.NiagaraEnum;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.annotations.Range;
import javax.baja.sys.BFrozenEnum;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraEnum(
   range = {@Range("all"), @Range("acked"), @Range("notAcked")}
)
public final class BAcknowledgmentFilter extends BFrozenEnum {
   public static final int ALL = 0;
   public static final int ACKED = 1;
   public static final int NOT_ACKED = 2;
   public static final BAcknowledgmentFilter all = new BAcknowledgmentFilter(0);
   public static final BAcknowledgmentFilter acked = new BAcknowledgmentFilter(1);
   public static final BAcknowledgmentFilter notAcked = new BAcknowledgmentFilter(2);
   public static final BAcknowledgmentFilter DEFAULT = all;
   public static final Type TYPE = Sys.loadType(BAcknowledgmentFilter.class);
   private static final BBacnetBitString ACKED_BITS = BBacnetBitString.make(new boolean[]{true, true, true});

   public static BAcknowledgmentFilter make(int ordinal) {
      return (BAcknowledgmentFilter)all.getRange().get(ordinal, false);
   }

   public static BAcknowledgmentFilter make(String tag) {
      return (BAcknowledgmentFilter)all.getRange().get(tag);
   }

   private BAcknowledgmentFilter(int ordinal) {
      super(ordinal);
   }

   public Type getType() {
      return TYPE;
   }

   public boolean filter(BBacnetBitString ackedTransitions) {
      if (ackedTransitions == null) {
         return false;
      } else {
         switch (this.getOrdinal()) {
            case 0:
               return true;
            case 1:
               return ACKED_BITS.equals(ackedTransitions);
            case 2:
               for (int i = 0; i < 3; i++) {
                  if (!ackedTransitions.getBit(i)) {
                     return true;
                  }
               }

               return false;
            default:
               throw new IllegalStateException();
         }
      }
   }
}
