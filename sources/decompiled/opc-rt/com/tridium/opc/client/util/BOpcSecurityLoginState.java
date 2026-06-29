package com.tridium.opc.client.util;

import javax.baja.nre.annotations.NiagaraEnum;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.annotations.Range;
import javax.baja.sys.BFrozenEnum;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraEnum(
   range = {@Range(
      value = "login",
      ordinal = 0
   ), @Range(
      value = "logoff",
      ordinal = 1
   )}
)
public final class BOpcSecurityLoginState extends BFrozenEnum {
   public static final int LOGIN = 0;
   public static final int LOGOFF = 1;
   public static final BOpcSecurityLoginState login = new BOpcSecurityLoginState(0);
   public static final BOpcSecurityLoginState logoff = new BOpcSecurityLoginState(1);
   public static final BOpcSecurityLoginState DEFAULT = login;
   public static final Type TYPE = Sys.loadType(BOpcSecurityLoginState.class);

   public static BOpcSecurityLoginState make(int ordinal) {
      return (BOpcSecurityLoginState)login.getRange().get(ordinal, false);
   }

   public static BOpcSecurityLoginState make(String tag) {
      return (BOpcSecurityLoginState)login.getRange().get(tag);
   }

   private BOpcSecurityLoginState(int ordinal) {
      super(ordinal);
   }

   public Type getType() {
      return TYPE;
   }
}
