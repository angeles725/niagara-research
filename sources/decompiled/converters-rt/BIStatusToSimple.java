package javax.baja.converters;

import javax.baja.nre.annotations.Adapter;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.status.BIStatus;
import javax.baja.status.BStatus;
import javax.baja.sys.BBoolean;
import javax.baja.sys.BObject;
import javax.baja.sys.BSimple;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.util.BConverter;

@NiagaraType(
   adapter = @Adapter(
      from = "baja:IStatus",
      to = "baja:Simple"
   )
)
@NiagaraProperties({@NiagaraProperty(
      name = "disabled",
      type = "BSimple",
      defaultValue = "BBoolean.FALSE"
   ), @NiagaraProperty(
      name = "fault",
      type = "BSimple",
      defaultValue = "BBoolean.FALSE"
   ), @NiagaraProperty(
      name = "down",
      type = "BSimple",
      defaultValue = "BBoolean.FALSE"
   ), @NiagaraProperty(
      name = "alarm",
      type = "BSimple",
      defaultValue = "BBoolean.FALSE"
   ), @NiagaraProperty(
      name = "stale",
      type = "BSimple",
      defaultValue = "BBoolean.FALSE"
   ), @NiagaraProperty(
      name = "overridden",
      type = "BSimple",
      defaultValue = "BBoolean.FALSE"
   ), @NiagaraProperty(
      name = "nullStatus",
      type = "BSimple",
      defaultValue = "BBoolean.FALSE"
   ), @NiagaraProperty(
      name = "unackedAlarm",
      type = "BSimple",
      defaultValue = "BBoolean.FALSE"
   ), @NiagaraProperty(
      name = "ok",
      type = "BSimple",
      defaultValue = "BBoolean.FALSE"
   )})
public final class BIStatusToSimple extends BConverter {
   public static final Property disabled = newProperty(0, BBoolean.FALSE, null);
   public static final Property fault = newProperty(0, BBoolean.FALSE, null);
   public static final Property down = newProperty(0, BBoolean.FALSE, null);
   public static final Property alarm = newProperty(0, BBoolean.FALSE, null);
   public static final Property stale = newProperty(0, BBoolean.FALSE, null);
   public static final Property overridden = newProperty(0, BBoolean.FALSE, null);
   public static final Property nullStatus = newProperty(0, BBoolean.FALSE, null);
   public static final Property unackedAlarm = newProperty(0, BBoolean.FALSE, null);
   public static final Property ok = newProperty(0, BBoolean.FALSE, null);
   public static final Type TYPE = Sys.loadType(BIStatusToSimple.class);

   public BSimple getDisabled() {
      return (BSimple)this.get(disabled);
   }

   public void setDisabled(BSimple v) {
      this.set(disabled, v, null);
   }

   public BSimple getFault() {
      return (BSimple)this.get(fault);
   }

   public void setFault(BSimple v) {
      this.set(fault, v, null);
   }

   public BSimple getDown() {
      return (BSimple)this.get(down);
   }

   public void setDown(BSimple v) {
      this.set(down, v, null);
   }

   public BSimple getAlarm() {
      return (BSimple)this.get(alarm);
   }

   public void setAlarm(BSimple v) {
      this.set(alarm, v, null);
   }

   public BSimple getStale() {
      return (BSimple)this.get(stale);
   }

   public void setStale(BSimple v) {
      this.set(stale, v, null);
   }

   public BSimple getOverridden() {
      return (BSimple)this.get(overridden);
   }

   public void setOverridden(BSimple v) {
      this.set(overridden, v, null);
   }

   public BSimple getNullStatus() {
      return (BSimple)this.get(nullStatus);
   }

   public void setNullStatus(BSimple v) {
      this.set(nullStatus, v, null);
   }

   public BSimple getUnackedAlarm() {
      return (BSimple)this.get(unackedAlarm);
   }

   public void setUnackedAlarm(BSimple v) {
      this.set(unackedAlarm, v, null);
   }

   public BSimple getOk() {
      return (BSimple)this.get(ok);
   }

   public void setOk(BSimple v) {
      this.set(ok, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public void init(BObject from, BObject to) {
      BSimple s = (BSimple)to;
      this.setDisabled(s);
      this.setFault(s);
      this.setDown(s);
      this.setAlarm(s);
      this.setStale(s);
      this.setOverridden(s);
      this.setNullStatus(s);
      this.setUnackedAlarm(s);
      this.setOk(s);
   }

   public BObject convert(BObject from, BObject to, Context cx) {
      BStatus s = ((BIStatus)from).getStatus();
      Type toType = to.getType();
      BSimple r = this.check(s, 1, this.getDisabled(), toType);
      if (r != null) {
         return r;
      } else {
         r = this.check(s, 2, this.getFault(), toType);
         if (r != null) {
            return r;
         } else {
            r = this.check(s, 4, this.getDown(), toType);
            if (r != null) {
               return r;
            } else {
               r = this.check(s, 8, this.getAlarm(), toType);
               if (r != null) {
                  return r;
               } else {
                  r = this.check(s, 16, this.getStale(), toType);
                  if (r != null) {
                     return r;
                  } else {
                     r = this.check(s, 32, this.getOverridden(), toType);
                     if (r != null) {
                        return r;
                     } else {
                        r = this.check(s, 64, this.getNullStatus(), toType);
                        if (r != null) {
                           return r;
                        } else {
                           r = this.check(s, 128, this.getUnackedAlarm(), toType);
                           if (r != null) {
                              return r;
                           } else {
                              r = this.check(this.getOk(), toType);
                              return (BObject)(r != null ? r : to);
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private BSimple check(BStatus status, int bit, BSimple value, Type toType) {
      return status.getBit(bit) && value.getType() == toType ? value : null;
   }

   private BSimple check(BSimple value, Type toType) {
      return value.getType() == toType ? value : null;
   }
}
