package com.tridium.opcUaServer.export;

import javax.baja.control.BBooleanPoint;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.status.BStatusBoolean;
import javax.baja.status.BStatusValue;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperty(
   name = "in",
   type = "BStatusBoolean",
   defaultValue = "new BStatusBoolean()",
   flags = 3
)
public class BBooleanExport extends BBooleanPoint implements BIOpcExport {
   public static final Property in = newProperty(3, new BStatusBoolean(), null);
   public static final Type TYPE = Sys.loadType(BBooleanExport.class);

   public BStatusBoolean getIn() {
      return (BStatusBoolean)this.get(in);
   }

   public void setIn(BStatusBoolean v) {
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
