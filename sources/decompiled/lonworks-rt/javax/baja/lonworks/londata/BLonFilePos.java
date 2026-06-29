package javax.baja.lonworks.londata;

import javax.baja.lonworks.enums.BLonElementType;
import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "pointer",
      type = "BLonInteger",
      defaultValue = "BLonInteger.DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementQualifiers.S32, null)")}
   ), @NiagaraProperty(
      name = "length",
      type = "BLonInteger",
      defaultValue = "BLonInteger.DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementQualifiers.make(BLonElementType.u16,0,65534,1,65535), null)")}
   )})
public class BLonFilePos extends BLonData {
   public static final Property pointer = newProperty(0, BLonInteger.DEFAULT, LonFacetsUtil.makeFacets(BLonElementQualifiers.S32, null));
   public static final Property length = newProperty(
      0, BLonInteger.DEFAULT, LonFacetsUtil.makeFacets(BLonElementQualifiers.make(BLonElementType.u16, 0.0F, 65534.0F, 1.0F, 65535.0F), null)
   );
   public static final Type TYPE = Sys.loadType(BLonFilePos.class);

   public BLonInteger getPointer() {
      return (BLonInteger)this.get(pointer);
   }

   public void setPointer(BLonInteger v) {
      this.set(pointer, v, null);
   }

   public BLonInteger getLength() {
      return (BLonInteger)this.get(length);
   }

   public void setLength(BLonInteger v) {
      this.set(length, v, null);
   }

   @Override
   public Type getType() {
      return TYPE;
   }
}
