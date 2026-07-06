package javax.baja.converters;

import javax.baja.nre.annotations.Adapter;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BEnum;
import javax.baja.sys.BIEnum;
import javax.baja.sys.BObject;
import javax.baja.sys.BSimple;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.util.BConverter;

@NiagaraType(
   adapter = @Adapter(
      from = "baja:IEnum",
      to = "baja:Simple"
   )
)
@NiagaraProperty(
   name = "map",
   type = "BEnumToSimpleMap",
   defaultValue = "BEnumToSimpleMap.NULL"
)
public final class BIEnumToSimple extends BConverter {
   public static final Property map = newProperty(0, BEnumToSimpleMap.NULL, null);
   public static final Type TYPE = Sys.loadType(BIEnumToSimple.class);

   public BEnumToSimpleMap getMap() {
      return (BEnumToSimpleMap)this.get(map);
   }

   public void setMap(BEnumToSimpleMap v) {
      this.set(map, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public void init(BObject from, BObject to) {
      this.setMap(BEnumToSimpleMap.make((BSimple)to));
   }

   public BObject convert(BObject from, BObject to, Context cx) {
      BEnum e = ((BIEnum)from).getEnum();
      BSimple value = this.getMap().get(e.getOrdinal());
      return (BObject)(value != null ? value : to);
   }
}
