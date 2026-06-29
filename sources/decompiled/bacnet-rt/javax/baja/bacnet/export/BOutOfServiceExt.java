package javax.baja.bacnet.export;

import javax.baja.control.BPointExtension;
import javax.baja.control.ext.BAbstractProxyExt;
import javax.baja.driver.point.BProxyExt;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.status.BStatusValue;
import javax.baja.sys.BBoolean;
import javax.baja.sys.BComponent;
import javax.baja.sys.BValue;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "outOfService",
      type = "boolean",
      defaultValue = "false"
   ), @NiagaraProperty(
      name = "presentValue",
      type = "BValue",
      defaultValue = "BBoolean.FALSE"
   )})
public class BOutOfServiceExt extends BPointExtension {
   public static final Property outOfService = newProperty(0, false, null);
   public static final Property presentValue = newProperty(0, BBoolean.FALSE, null);
   public static final Type TYPE = Sys.loadType(BOutOfServiceExt.class);
   BIBacnetExportObject export;
   boolean isCommandable;

   public boolean getOutOfService() {
      return this.getBoolean(outOfService);
   }

   public void setOutOfService(boolean v) {
      this.setBoolean(outOfService, v, null);
   }

   public BValue getPresentValue() {
      return this.get(presentValue);
   }

   public void setPresentValue(BValue v) {
      this.set(presentValue, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public void started() throws Exception {
      super.started();
      this.setPresentValue(this.getParentPoint().getOutStatusValue().getValueValue());
   }

   public void changed(Property p, Context cx) {
      super.changed(p, cx);
      if (this.isRunning()) {
         if (p.equals(outOfService)) {
            BComponent c = (BComponent)this.export;
            this.setPresentValue(this.getParentPoint().getOutStatusValue().getValueValue());
            int flags = c.getFlags(c.getSlot("reliability"));
            if (this.getOutOfService()) {
               flags &= -2;
            } else {
               flags |= 1;
            }

            c.setFlags(c.getSlot("reliability"), flags);
            if (this.export instanceof BBacnetEventSource) {
               ((BBacnetEventSource)this.export).statusChanged();
            }
         }

         this.executePoint();
         if (this.export instanceof BIBacnetCovSource) {
            ((BIBacnetCovSource)this.export).checkCov();
         }
      }
   }

   public void onExecute(BStatusValue working, Context cx) {
      if (this.getOutOfService()) {
         if (this.isCommandable) {
            BAbstractProxyExt apx = this.getParentPoint().getProxyExt();
            if (apx instanceof BProxyExt) {
               BProxyExt px = (BProxyExt)apx;
               px.writeReset();
            }
         }

         working.setValueValue(this.getPresentValue());
      }
   }

   public BIBacnetExportObject getExport() {
      return this.export;
   }

   public void setExport(BIBacnetExportObject exp) {
      this.export = exp;
   }

   public void setCommandable(boolean commandable) {
      this.isCommandable = commandable;
   }
}
