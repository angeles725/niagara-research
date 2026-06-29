package com.tridium.opc.client.util;

import javax.baja.nre.annotations.NiagaraEnum;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.annotations.Range;
import javax.baja.sys.BFrozenEnum;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraEnum(
   range = {@Range("unsubscribed"), @Range("pending"), @Range("subscribed")}
)
public final class BOpcSubscription extends BFrozenEnum {
   public static final int UNSUBSCRIBED = 0;
   public static final int PENDING = 1;
   public static final int SUBSCRIBED = 2;
   public static final BOpcSubscription unsubscribed = new BOpcSubscription(0);
   public static final BOpcSubscription pending = new BOpcSubscription(1);
   public static final BOpcSubscription subscribed = new BOpcSubscription(2);
   public static final BOpcSubscription DEFAULT = unsubscribed;
   public static final Type TYPE = Sys.loadType(BOpcSubscription.class);

   public static BOpcSubscription make(int ordinal) {
      return (BOpcSubscription)unsubscribed.getRange().get(ordinal, false);
   }

   public static BOpcSubscription make(String tag) {
      return (BOpcSubscription)unsubscribed.getRange().get(tag);
   }

   private BOpcSubscription(int ordinal) {
      super(ordinal);
   }

   public Type getType() {
      return TYPE;
   }

   public boolean isPending() {
      return this == pending;
   }

   public boolean isSubscribed() {
      return this == subscribed;
   }

   public boolean isUnsubscribed() {
      return this == unsubscribed;
   }
}
