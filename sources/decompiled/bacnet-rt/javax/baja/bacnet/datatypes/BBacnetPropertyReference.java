package javax.baja.bacnet.datatypes;

import javax.baja.bacnet.enums.BBacnetPropertyIdentifier;
import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.io.AsnInput;
import javax.baja.bacnet.io.AsnOutput;
import javax.baja.bacnet.io.PropertyReference;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BStruct;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "propertyId",
      type = "int",
      defaultValue = "BBacnetPropertyIdentifier.PRESENT_VALUE"
   ), @NiagaraProperty(
      name = "propertyArrayIndex",
      type = "int",
      defaultValue = "NOT_USED"
   )})
public final class BBacnetPropertyReference extends BStruct implements PropertyReference, BIBacnetDataType {
   public static final Property propertyId = newProperty(0, 85, null);
   public static final Property propertyArrayIndex = newProperty(0, -1, null);
   public static final Type TYPE = Sys.loadType(BBacnetPropertyReference.class);
   public static final int PROPERTY_ID_TAG = 0;
   public static final int PROPERTY_ARRAY_INDEX_TAG = 1;

   @Override
   public int getPropertyId() {
      return this.getInt(propertyId);
   }

   public void setPropertyId(int v) {
      this.setInt(propertyId, v, null);
   }

   @Override
   public int getPropertyArrayIndex() {
      return this.getInt(propertyArrayIndex);
   }

   public void setPropertyArrayIndex(int v) {
      this.setInt(propertyArrayIndex, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BBacnetPropertyReference() {
   }

   public BBacnetPropertyReference(int propertyId) {
      this.setPropertyId(propertyId);
   }

   public BBacnetPropertyReference(int propertyId, int propertyArrayIndex) {
      this.setPropertyId(propertyId);
      this.setPropertyArrayIndex(propertyArrayIndex);
   }

   public boolean isPropertyArrayIndexUsed() {
      return this.getPropertyArrayIndex() != -1;
   }

   @Override
   public void writeAsn(AsnOutput out) {
      out.writeEnumerated(0, this.getPropertyId());
      if (this.isPropertyArrayIndexUsed()) {
         out.writeUnsignedInteger(1, this.getPropertyArrayIndex());
      }
   }

   @Override
   public void readAsn(AsnInput in) throws AsnException {
      int propertyId = in.readEnumerated(0);
      in.peekTag();
      int propertyArrayIndex = in.isValueTag(1) ? in.readUnsignedInt(1) : -1;
      this.setInt(BBacnetPropertyReference.propertyId, propertyId, noWrite);
      this.setInt(BBacnetPropertyReference.propertyArrayIndex, propertyArrayIndex, noWrite);
   }

   public String toString(Context cx) {
      return BBacnetPropertyIdentifier.tag(this.getPropertyId()) + "[" + this.getPropertyArrayIndex() + "]";
   }

   public String toDebugString() {
      StringBuilder sb = new StringBuilder(32);
      sb.append("\n  " + BBacnetPropertyIdentifier.tag(this.getPropertyId()));
      if (this.isPropertyArrayIndexUsed()) {
         sb.append("[" + this.getPropertyArrayIndex() + "]");
      }

      return sb.toString();
   }
}
