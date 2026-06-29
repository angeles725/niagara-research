package javax.baja.bacnet.datatypes;

import java.util.ArrayList;
import java.util.List;
import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.io.AsnInput;
import javax.baja.bacnet.io.AsnOutput;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BComponent;
import javax.baja.sys.BObject;
import javax.baja.sys.BValue;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.SlotCursor;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperty(
   name = "objectIdentifier",
   type = "BBacnetObjectIdentifier",
   defaultValue = "BBacnetObjectIdentifier.DEFAULT"
)
public class BReadAccessSpecification extends BComponent implements BIBacnetDataType {
   public static final Property objectIdentifier = newProperty(0, BBacnetObjectIdentifier.DEFAULT, null);
   public static final Type TYPE = Sys.loadType(BReadAccessSpecification.class);
   public static final int OBJECT_ID_TAG = 0;
   public static final int LIST_OF_PROPERTY_REFERENCES_TAG = 1;

   public BBacnetObjectIdentifier getObjectIdentifier() {
      return (BBacnetObjectIdentifier)this.get(objectIdentifier);
   }

   public void setObjectIdentifier(BBacnetObjectIdentifier v) {
      this.set(objectIdentifier, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BReadAccessSpecification() {
   }

   public BReadAccessSpecification(BBacnetPropertyReference ref) {
      this.add(null, ref);
   }

   public BReadAccessSpecification(BBacnetPropertyReference[] refs) {
      if (refs != null) {
         for (int i = 0; i < refs.length; i++) {
            this.add(null, refs[i]);
         }
      }
   }

   public boolean isChildLegal(BComponent child) {
      return false;
   }

   public String toString(Context cx) {
      this.loadSlots();
      Object[] a = this.getChildren(BBacnetPropertyReference.class);
      StringBuilder sb = new StringBuilder(this.getObjectIdentifier().toString(cx));
      sb.append("{");

      for (int i = 0; i < a.length; i++) {
         sb.append(((BObject)a[i]).toString(cx)).append(',');
      }

      sb.append('}');
      return sb.toString();
   }

   @Override
   public void writeAsn(AsnOutput out) {
      out.writeObjectIdentifier(0, this.getObjectIdentifier());
      out.writeOpeningTag(1);
      SlotCursor<Property> sc = this.getProperties();

      while (sc.next(BBacnetPropertyReference.class)) {
         BBacnetPropertyReference propRef = (BBacnetPropertyReference)sc.get();
         propRef.writeAsn(out);
      }

      out.writeClosingTag(1);
   }

   @Override
   public void readAsn(AsnInput in) throws AsnException {
      BBacnetObjectIdentifier objectIdentifier = in.readObjectIdentifier(0);
      in.skipOpeningTag(1);
      int tag = in.peekTag();

      List<BBacnetPropertyReference> propRefs;
      for (propRefs = new ArrayList<>(); !in.isClosingTag(1); tag = in.peekTag()) {
         if (tag == -1) {
            throw new AsnException("Invalid tag: " + tag);
         }

         BBacnetPropertyReference propRef = new BBacnetPropertyReference();
         propRef.readAsn(in);
         propRefs.add(propRef);
      }

      in.skipClosingTag(1);
      this.set(BReadAccessSpecification.objectIdentifier, objectIdentifier, noWrite);
      this.removeAll(noWrite);
      int length = propRefs.size();

      for (int i = 0; i < length; i++) {
         this.add("propertyReference" + (i + 1), (BValue)propRefs.get(i), noWrite);
      }
   }
}
