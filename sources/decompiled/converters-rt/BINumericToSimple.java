package javax.baja.converters;

import javax.baja.nre.annotations.Adapter;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BINumeric;
import javax.baja.sys.BObject;
import javax.baja.sys.BSimple;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.util.BConverter;

@NiagaraType(
   adapter = @Adapter(
      from = "baja:INumeric",
      to = "baja:Simple"
   )
)
@NiagaraProperty(
   name = "map",
   type = "BNumericToSimpleMap",
   defaultValue = "BNumericToSimpleMap.NULL"
)
public final class BINumericToSimple extends BConverter {
   public static final Property map = newProperty(0, BNumericToSimpleMap.NULL, null);
   public static final Type TYPE = Sys.loadType(BINumericToSimple.class);

   public BNumericToSimpleMap getMap() {
      return (BNumericToSimpleMap)this.get(map);
   }

   public void setMap(BNumericToSimpleMap v) {
      this.set(map, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public void init(BObject from, BObject to) {
      this.setMap(BNumericToSimpleMap.make((BSimple)to));
   }

   public BObject convert(BObject from, BObject to, Context cx) {
      double numeric = ((BINumeric)from).getNumeric();
      BSimple value = this.getMap().get(numeric);
      return (BObject)(value != null ? value : to);
   }
}
