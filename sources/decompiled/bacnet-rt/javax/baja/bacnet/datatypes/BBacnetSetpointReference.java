package javax.baja.bacnet.datatypes;

import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.io.AsnInput;
import javax.baja.bacnet.io.AsnOutput;
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
      name = "referenceUsed",
      type = "boolean",
      defaultValue = "false",
      flags = 4
   ), @NiagaraProperty(
      name = "reference",
      type = "BBacnetObjectPropertyReference",
      defaultValue = "new BBacnetObjectPropertyReference()"
   )})
public final class BBacnetSetpointReference extends BStruct implements BIBacnetDataType {
   public static final Property referenceUsed = newProperty(4, false, null);
   public static final Property reference = newProperty(0, new BBacnetObjectPropertyReference(), null);
   public static final Type TYPE = Sys.loadType(BBacnetSetpointReference.class);
   public static final int SETPOINT_REFERENCE_TAG = 0;

   public boolean getReferenceUsed() {
      return this.getBoolean(referenceUsed);
   }

   public void setReferenceUsed(boolean v) {
      this.setBoolean(referenceUsed, v, null);
   }

   public BBacnetObjectPropertyReference getReference() {
      return (BBacnetObjectPropertyReference)this.get(reference);
   }

   public void setReference(BBacnetObjectPropertyReference v) {
      this.set(reference, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BBacnetSetpointReference() {
   }

   public BBacnetSetpointReference(BBacnetObjectPropertyReference setpointReference) {
      this.setSetpointReference(setpointReference);
   }

   public String toString(Context context) {
      StringBuilder sb = new StringBuilder("setpointReference:");
      if (this.getReferenceUsed()) {
         sb.append(this.getReference().toString(context));
      } else {
         sb.append("empty");
      }

      return sb.toString();
   }

   public BBacnetObjectPropertyReference getSetpointReference() {
      return this.getReferenceUsed() ? this.getReference() : null;
   }

   public void setSetpointReference(BBacnetObjectPropertyReference setpointReference) {
      this.setSetpointReference(setpointReference, null);
   }

   public void setSetpointReference(BBacnetObjectPropertyReference setpointReference, Context cx) {
      if (setpointReference == null) {
         this.setBoolean(referenceUsed, false, cx);
      } else {
         this.setBoolean(referenceUsed, true, cx);
         this.set(reference, setpointReference, cx);
      }
   }

   @Override
   public void writeAsn(AsnOutput out) {
      if (this.getReferenceUsed()) {
         out.writeOpeningTag(0);
         this.getReference().writeAsn(out);
         out.writeClosingTag(0);
      }
   }

   @Override
   public void readAsn(AsnInput in) throws AsnException {
      BBacnetObjectPropertyReference reference = null;
      in.peekTag();
      if (in.isOpeningTag(0)) {
         in.skipTag();
         in.peekTag();
         if (!in.isClosingTag(0)) {
            reference = new BBacnetObjectPropertyReference();
            reference.readAsn(in);
         }

         in.skipClosingTag(0);
      }

      this.setSetpointReference(reference, noWrite);
   }
}
