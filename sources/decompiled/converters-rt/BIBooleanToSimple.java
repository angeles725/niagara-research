package javax.baja.converters;

import javax.baja.nre.annotations.Adapter;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BBoolean;
import javax.baja.sys.BIBoolean;
import javax.baja.sys.BObject;
import javax.baja.sys.BSimple;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.util.BConverter;

@NiagaraType(
   adapter = @Adapter(
      from = "baja:IBoolean",
      to = "baja:Simple"
   )
)
@NiagaraProperties({@NiagaraProperty(
      name = "trueValue",
      type = "BSimple",
      defaultValue = "BBoolean.TRUE"
   ), @NiagaraProperty(
      name = "falseValue",
      type = "BSimple",
      defaultValue = "BBoolean.FALSE"
   )})
public final class BIBooleanToSimple extends BConverter {
   public static final Property trueValue = newProperty(0, BBoolean.TRUE, null);
   public static final Property falseValue = newProperty(0, BBoolean.FALSE, null);
   public static final Type TYPE = Sys.loadType(BIBooleanToSimple.class);

   public BSimple getTrueValue() {
      return (BSimple)this.get(trueValue);
   }

   public void setTrueValue(BSimple v) {
      this.set(trueValue, v, null);
   }

   public BSimple getFalseValue() {
      return (BSimple)this.get(falseValue);
   }

   public void setFalseValue(BSimple v) {
      this.set(falseValue, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public void init(BObject from, BObject to) {
      this.setTrueValue((BSimple)to);
      this.setFalseValue((BSimple)to);
   }

   public BObject convert(BObject from, BObject to, Context cx) {
      boolean bool = ((BIBoolean)from).getBoolean();
      BSimple v;
      if (bool) {
         v = this.getTrueValue();
      } else {
         v = this.getFalseValue();
      }

      return (BObject)(v.getType() == to.getType() ? v : to);
   }
}
