package com.tridium.template;

import javax.baja.nre.annotations.NiagaraEnum;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.annotations.Range;
import javax.baja.sys.BFrozenEnum;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraEnum(
   range = {@Range(
      value = "draft",
      ordinal = 10
   ), @Range(
      value = "ready",
      ordinal = 20
   )},
   defaultValue = "draft"
)
public final class BTemplateState extends BFrozenEnum {
   public static final int DRAFT = 10;
   public static final int READY = 20;
   public static final BTemplateState draft = new BTemplateState(10);
   public static final BTemplateState ready = new BTemplateState(20);
   public static final BTemplateState DEFAULT = draft;
   public static final Type TYPE = Sys.loadType(BTemplateState.class);

   public static BTemplateState make(int ordinal) {
      return (BTemplateState)draft.getRange().get(ordinal, false);
   }

   public static BTemplateState make(String tag) {
      return (BTemplateState)draft.getRange().get(tag);
   }

   private BTemplateState(int ordinal) {
      super(ordinal);
   }

   public Type getType() {
      return TYPE;
   }
}
