package javax.baja.bacnet.datatypes;

import java.io.IOException;
import java.util.StringTokenizer;
import javax.baja.bacnet.enums.BBacnetPropertyIdentifier;
import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.io.AsnInput;
import javax.baja.bacnet.io.AsnOutput;
import javax.baja.bacnet.io.PropertyReference;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BObject;
import javax.baja.sys.BStruct;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "objectId",
      type = "BBacnetObjectIdentifier",
      defaultValue = "BBacnetObjectIdentifier.DEFAULT"
   ), @NiagaraProperty(
      name = "propertyId",
      type = "int",
      defaultValue = "BBacnetPropertyIdentifier.PRESENT_VALUE"
   ), @NiagaraProperty(
      name = "propertyArrayIndex",
      type = "int",
      defaultValue = "NOT_USED"
   )})
public final class BBacnetObjectPropertyReference extends BStruct implements BIBacnetDataType, PropertyReference {
   public static final Property objectId = newProperty(0, BBacnetObjectIdentifier.DEFAULT, null);
   public static final Property propertyId = newProperty(0, 85, null);
   public static final Property propertyArrayIndex = newProperty(0, -1, null);
   public static final Type TYPE = Sys.loadType(BBacnetObjectPropertyReference.class);
   public static final int OBJECT_ID_TAG = 0;
   public static final int PROPERTY_ID_TAG = 1;
   public static final int PROPERTY_ARRAY_INDEX_TAG = 2;

   public BBacnetObjectIdentifier getObjectId() {
      return (BBacnetObjectIdentifier)this.get(objectId);
   }

   public void setObjectId(BBacnetObjectIdentifier v) {
      this.set(objectId, v, null);
   }

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

   public BBacnetObjectPropertyReference() {
   }

   public BBacnetObjectPropertyReference(BBacnetObjectIdentifier objectId) {
      this.setObjectId(objectId);
   }

   public BBacnetObjectPropertyReference(BBacnetObjectIdentifier objectId, int propertyId) {
      this.setObjectId(objectId);
      this.setPropertyId(propertyId);
   }

   public BBacnetObjectPropertyReference(BBacnetObjectIdentifier objectId, int propertyId, int propertyArrayIndex) {
      this.setObjectId(objectId);
      this.setPropertyId(propertyId);
      this.setPropertyArrayIndex(propertyArrayIndex);
   }

   public boolean isPropertyArrayIndexUsed() {
      return this.getPropertyArrayIndex() != -1;
   }

   @Override
   public void writeAsn(AsnOutput out) {
      out.writeObjectIdentifier(0, this.getObjectId());
      out.writeEnumerated(1, this.getPropertyId());
      if (this.isPropertyArrayIndexUsed()) {
         out.writeUnsignedInteger(2, this.getPropertyArrayIndex());
      }
   }

   @Override
   public void readAsn(AsnInput in) throws AsnException {
      BBacnetObjectIdentifier objectId = in.readObjectIdentifier(0);
      int propertyId = in.readEnumerated(1);
      in.peekTag();
      int propertyArrayIndex = in.isValueTag(2) ? in.readUnsignedInt(2) : -1;
      this.set(BBacnetObjectPropertyReference.objectId, objectId, noWrite);
      this.setInt(BBacnetObjectPropertyReference.propertyId, propertyId, noWrite);
      this.setInt(BBacnetObjectPropertyReference.propertyArrayIndex, propertyArrayIndex, noWrite);
   }

   public String toString(Context cx) {
      StringBuilder sb = new StringBuilder();
      sb.append(this.getObjectId().toString(cx)).append('_').append(BBacnetPropertyIdentifier.tag(this.getPropertyId()));
      if (cx == null || !cx.equals(nameContext)) {
         sb.append('[').append(this.getPropertyArrayIndex()).append(']');
      } else if (this.isPropertyArrayIndexUsed()) {
         sb.append('_').append(this.getPropertyArrayIndex());
      }

      return sb.toString();
   }

   public String toDebugString() {
      StringBuilder sb = new StringBuilder(32);
      sb.append("\n  " + this.getObjectId().toString());
      sb.append("\n  " + BBacnetPropertyIdentifier.tag(this.getPropertyId()));
      if (this.isPropertyArrayIndexUsed()) {
         sb.append("[" + this.getPropertyArrayIndex() + "]");
      }

      return sb.toString();
   }

   public String encodeToString() throws IOException {
      return this.getObjectId().encodeToString() + '|' + this.getPropertyId() + '|' + this.getPropertyArrayIndex();
   }

   public BObject decodeFromString(String s) throws IOException {
      try {
         StringTokenizer st = new StringTokenizer(s, "|");
         BBacnetObjectIdentifier temp = BBacnetObjectIdentifier.DEFAULT;
         BBacnetObjectIdentifier newObjectId = (BBacnetObjectIdentifier)temp.decodeFromString(st.nextToken());
         int propId = Integer.parseInt(st.nextToken());
         int propArrayIndex = Integer.parseInt(st.nextToken());
         return new BBacnetObjectPropertyReference(newObjectId, propId, propArrayIndex);
      } catch (Exception var7) {
         throw new IOException("Error decoding BBacnetObjectPropertyReference " + s);
      }
   }
}
