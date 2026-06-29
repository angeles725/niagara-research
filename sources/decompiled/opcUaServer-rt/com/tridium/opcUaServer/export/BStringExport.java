package com.tridium.opcUaServer.export;

import javax.baja.control.BStringPoint;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.status.BStatusString;
import javax.baja.status.BStatusValue;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperty(
   name = "in",
   type = "BStatusString",
   defaultValue = "new BStatusString()"
)
public class BStringExport extends BStringPoint implements BIOpcExport {
   public static final Property in = newProperty(0, new BStatusString(), null);
   public static final Type TYPE = Sys.loadType(BStringExport.class);

   public BStatusString getIn() {
      return (BStatusString)this.get(in);
   }

   public void setIn(BStatusString v) {
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
