package com.tridium.opcUaServer.export;

import javax.baja.control.BEnumPoint;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.status.BStatusEnum;
import javax.baja.status.BStatusValue;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperty(
   name = "in",
   type = "BStatusEnum",
   defaultValue = "new BStatusEnum()"
)
public class BEnumExport extends BEnumPoint implements BIOpcExport {
   public static final Property in = newProperty(0, new BStatusEnum(), null);
   public static final Type TYPE = Sys.loadType(BEnumExport.class);

   public BStatusEnum getIn() {
      return (BStatusEnum)this.get(in);
   }

   public void setIn(BStatusEnum v) {
      this.set(in, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public void onExecute(BStatusValue out, Context cx) {
      out.copyFrom(this.getIn());
   }

   public void changed(Property prop, Context cx) {
      if (this.isRunning() && prop.equals(in)) {
         this.updateValue((BStatusValue)this.getIn().newCopy(), cx);
         super.changed(prop, cx);
      }
   }
}
