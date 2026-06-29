package com.tridium.opc.client.util;

import javax.baja.nre.annotations.NiagaraEnum;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.annotations.Range;
import javax.baja.sys.BFrozenEnum;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraEnum(
   range = {@Range("attaching"), @Range("attached"), @Range("detaching"), @Range("detached")}
)
public final class BOpcState extends BFrozenEnum {
   public static final int ATTACHING = 0;
   public static final int ATTACHED = 1;
   public static final int DETACHING = 2;
   public static final int DETACHED = 3;
   public static final BOpcState attaching = new BOpcState(0);
   public static final BOpcState attached = new BOpcState(1);
   public static final BOpcState detaching = new BOpcState(2);
   public static final BOpcState detached = new BOpcState(3);
   public static final BOpcState DEFAULT = attaching;
   public static final Type TYPE = Sys.loadType(BOpcState.class);

   public static BOpcState make(int ordinal) {
      return (BOpcState)attaching.getRange().get(ordinal, false);
   }

   public static BOpcState make(String tag) {
      return (BOpcState)attaching.getRange().get(tag);
   }

   private BOpcState(int ordinal) {
      super(ordinal);
   }

   public Type getType() {
      return TYPE;
   }

   public boolean isAttached() {
      return this == attached;
   }

   public boolean isAttaching() {
      return this == attaching;
   }

   public boolean isDetached() {
      return this == detached;
   }

   public boolean isDetaching() {
      return this == detaching;
   }

   public boolean isDisengaged() {
      return this.isDetached() || this.isDetaching();
   }

   public boolean isEngaged() {
      return this.isAttached() || this.isAttaching();
   }
}
