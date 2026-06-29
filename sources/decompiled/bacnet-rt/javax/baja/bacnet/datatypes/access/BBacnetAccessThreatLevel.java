package javax.baja.bacnet.datatypes.access;

import javax.baja.bacnet.datatypes.BIBacnetDataType;
import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.io.AsnInput;
import javax.baja.bacnet.io.AsnOutput;
import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BFacets;
import javax.baja.sys.BStruct;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperty(
   name = "accessThreatLevel",
   type = "int",
   defaultValue = "0",
   facets = {@Facet("BFacets.makeInt(0, 100)")}
)
public final class BBacnetAccessThreatLevel extends BStruct implements BIBacnetDataType {
   public static final Property accessThreatLevel = newProperty(0, 0, BFacets.makeInt(0, 100));
   public static final Type TYPE = Sys.loadType(BBacnetAccessThreatLevel.class);

   public int getAccessThreatLevel() {
      return this.getInt(accessThreatLevel);
   }

   public void setAccessThreatLevel(int v) {
      this.setInt(accessThreatLevel, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BBacnetAccessThreatLevel() {
   }

   public BBacnetAccessThreatLevel(int accessThreatLevel) {
      this.setAccessThreatLevel(accessThreatLevel);
   }

   public String toString(Context context) {
      StringBuilder sb = new StringBuilder("ATL:");
      sb.append(this.getAccessThreatLevel());
      return sb.toString();
   }

   @Override
   public void writeAsn(AsnOutput out) {
      out.writeUnsignedInteger(this.getAccessThreatLevel());
   }

   @Override
   public void readAsn(AsnInput in) throws AsnException {
      this.set(accessThreatLevel, in.readUnsigned(), noWrite);
   }
}
