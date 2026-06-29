package com.tridium.bacnet.enums;

import javax.baja.nre.annotations.NiagaraEnum;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.annotations.Range;
import javax.baja.sys.BFrozenEnum;
import javax.baja.sys.Context;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.util.Lexicon;

@NiagaraType
@NiagaraEnum(
   range = {@Range(
      value = "ms_20",
      ordinal = 20
   ), @Range(
      value = "ms_35",
      ordinal = 35
   ), @Range(
      value = "ms_85",
      ordinal = 85
   )}
)
public final class BBacnetMstpUsageTimeout extends BFrozenEnum {
   public static final int MS_20 = 20;
   public static final int MS_35 = 35;
   public static final int MS_85 = 85;
   public static final BBacnetMstpUsageTimeout ms_20 = new BBacnetMstpUsageTimeout(20);
   public static final BBacnetMstpUsageTimeout ms_35 = new BBacnetMstpUsageTimeout(35);
   public static final BBacnetMstpUsageTimeout ms_85 = new BBacnetMstpUsageTimeout(85);
   public static final BBacnetMstpUsageTimeout DEFAULT = ms_20;
   public static final Type TYPE = Sys.loadType(BBacnetMstpUsageTimeout.class);

   public static BBacnetMstpUsageTimeout make(int ordinal) {
      return (BBacnetMstpUsageTimeout)ms_20.getRange().get(ordinal, false);
   }

   public static BBacnetMstpUsageTimeout make(String tag) {
      return (BBacnetMstpUsageTimeout)ms_20.getRange().get(tag);
   }

   private BBacnetMstpUsageTimeout(int ordinal) {
      super(ordinal);
   }

   public Type getType() {
      return TYPE;
   }

   public String getDisplayTag(Context cx) {
      try {
         String tag = this.getTag();
         String displayTag = Lexicon.make(TYPE.getModule(), cx).get(TYPE.getTypeName() + "." + tag);
         if (displayTag != null) {
            return displayTag;
         }
      } catch (Exception var4) {
      }

      return super.getDisplayTag(cx);
   }
}
