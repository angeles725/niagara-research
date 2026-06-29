package com.tridium.opcUaServer.export;

import javax.baja.control.BNumericPoint;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.status.BStatusNumeric;
import javax.baja.status.BStatusValue;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperty(
   name = "in",
   type = "BStatusNumeric",
   defaultValue = "new BStatusNumeric()"
)
public class BNumericExport extends BNumericPoint implements BIOpcExport {
   public static final Property in = newProperty(0, new BStatusNumeric(), null);
   public static final Type TYPE = Sys.loadType(BNumericExport.class);

   public BStatusNumeric getIn() {
      return (BStatusNumeric)this.get(in);
   }

   public void setIn(BStatusNumeric v) {
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
