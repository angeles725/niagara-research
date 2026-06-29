package javax.baja.lonworks.datatypes;

import javax.baja.naming.BOrd;
import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BBoolean;
import javax.baja.sys.BFacets;
import javax.baja.sys.BStruct;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperty(
   name = "file",
   type = "BOrd",
   defaultValue = "BOrd.NULL",
   facets = {@Facet("BFacets.make(\"allowLocalAccess\", BBoolean.TRUE)")}
)
public class BLocalImportXmlParameter extends BStruct {
   public static final Property file = newProperty(0, BOrd.NULL, BFacets.make("allowLocalAccess", BBoolean.TRUE));
   public static final Type TYPE = Sys.loadType(BLocalImportXmlParameter.class);

   public BOrd getFile() {
      return (BOrd)this.get(file);
   }

   public void setFile(BOrd v) {
      this.set(file, v, null);
   }

   public Type getType() {
      return TYPE;
   }
}
