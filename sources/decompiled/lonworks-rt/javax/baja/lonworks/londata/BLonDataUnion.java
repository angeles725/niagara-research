package javax.baja.lonworks.londata;

import javax.baja.lonworks.datatypes.BUnionQualifiers;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BObject;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperty(
   name = "unionQuals",
   type = "BUnionQualifiers",
   defaultValue = "new BUnionQualifiers()",
   flags = 4
)
public class BLonDataUnion extends BLonData {
   public static final Property unionQuals = newProperty(4, new BUnionQualifiers(), null);
   public static final Type TYPE = Sys.loadType(BLonDataUnion.class);

   public BUnionQualifiers getUnionQuals() {
      return (BUnionQualifiers)this.get(unionQuals);
   }

   public void setUnionQuals(BUnionQualifiers v) {
      this.set(unionQuals, v, null);
   }

   @Override
   public Type getType() {
      return TYPE;
   }

   @Override
   public boolean hasEquivalentElements(BLonData ld) {
      return !super.hasEquivalentElements(ld) ? false : ((BLonDataUnion)ld).getUnionQuals().equivalent(this.getUnionQuals());
   }

   @Override
   public boolean isUnion() {
      return true;
   }

   public Property[] getActiveProperties() {
      return this.getActiveProperties(this.get(this.getProperty(this.getUnionQuals().getConditionProp())));
   }

   public Property[] getActiveProperties(BObject condVal) {
      return this.getUnionQuals().getActiveProperties(condVal);
   }

   @Override
   Property[] getActiveProps() {
      return this.getActiveProperties();
   }
}
