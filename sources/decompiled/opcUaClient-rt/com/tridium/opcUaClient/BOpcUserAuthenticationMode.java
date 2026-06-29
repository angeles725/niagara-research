package com.tridium.opcUaClient;

import javax.baja.nre.annotations.NiagaraEnum;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.annotations.Range;
import javax.baja.sys.BFrozenEnum;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraEnum(
   range = {@Range("anonymous"), @Range("userNameAndPassword"), @Range("certificate")},
   defaultValue = "userNameAndPassword"
)
public final class BOpcUserAuthenticationMode extends BFrozenEnum {
   public static final int ANONYMOUS = 0;
   public static final int USER_NAME_AND_PASSWORD = 1;
   public static final int CERTIFICATE = 2;
   public static final BOpcUserAuthenticationMode anonymous = new BOpcUserAuthenticationMode(0);
   public static final BOpcUserAuthenticationMode userNameAndPassword = new BOpcUserAuthenticationMode(1);
   public static final BOpcUserAuthenticationMode certificate = new BOpcUserAuthenticationMode(2);
   public static final BOpcUserAuthenticationMode DEFAULT = userNameAndPassword;
   public static final Type TYPE = Sys.loadType(BOpcUserAuthenticationMode.class);

   public static BOpcUserAuthenticationMode make(int ordinal) {
      return (BOpcUserAuthenticationMode)anonymous.getRange().get(ordinal, false);
   }

   public static BOpcUserAuthenticationMode make(String tag) {
      return (BOpcUserAuthenticationMode)anonymous.getRange().get(tag);
   }

   private BOpcUserAuthenticationMode(int ordinal) {
      super(ordinal);
   }

   public Type getType() {
      return TYPE;
   }
}
