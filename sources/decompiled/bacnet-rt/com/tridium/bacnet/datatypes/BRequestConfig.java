package com.tridium.bacnet.datatypes;

import javax.baja.bacnet.BacnetConst;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BStruct;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public abstract class BRequestConfig extends BStruct implements BacnetConst {
   public static final Type TYPE = Sys.loadType(BRequestConfig.class);
   @Deprecated
   public static final int DISCOVER_DEVICES = 0;
   @Deprecated
   public static final int WHO_IS = 1;
   @Deprecated
   public static final int WHO_HAS = 2;
   @Deprecated
   public static final int TIME_SYNCH = 3;
   @Deprecated
   protected int requestType;

   public Type getType() {
      return TYPE;
   }

   @Deprecated
   public int getRequestType() {
      return this.requestType;
   }
}
