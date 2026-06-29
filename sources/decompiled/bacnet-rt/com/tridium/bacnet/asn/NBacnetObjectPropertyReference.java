package com.tridium.bacnet.asn;

import javax.baja.bacnet.BacnetConst;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.enums.BBacnetPropertyIdentifier;
import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.io.AsnInput;
import javax.baja.bacnet.io.AsnOutput;

@Deprecated
public class NBacnetObjectPropertyReference implements BacnetConst {
   public static final int OBJECT_ID_TAG = 0;
   public static final int PROPERTY_ID_TAG = 1;
   public static final int PROPERTY_ARRAY_INDEX_TAG = 2;
   private BBacnetObjectIdentifier objectId;
   private int propertyId;
   private int propertyArrayIndex;

   public NBacnetObjectPropertyReference() {
      this(BBacnetObjectIdentifier.DEFAULT, -1, -1);
   }

   public NBacnetObjectPropertyReference(BBacnetObjectIdentifier objectId) {
      this(objectId, -1, -1);
   }

   public NBacnetObjectPropertyReference(BBacnetObjectIdentifier objectId, int propertyId) {
      this(objectId, propertyId, -1);
   }

   public NBacnetObjectPropertyReference(BBacnetObjectIdentifier objectId, int propertyId, int propertyArrayIndex) {
      this.objectId = objectId;
      this.propertyId = propertyId;
      this.propertyArrayIndex = propertyArrayIndex;
   }

   public BBacnetObjectIdentifier getObjectId() {
      return this.objectId;
   }

   public void setObjectId(BBacnetObjectIdentifier objectId) {
      this.objectId = objectId;
   }

   public int getPropertyId() {
      return this.propertyId;
   }

   public void setPropertyId(int propertyId) {
      this.propertyId = propertyId;
   }

   public boolean isPropertyArrayIndexUsed() {
      return this.propertyArrayIndex != -1;
   }

   public int getPropertyArrayIndex() {
      return this.propertyArrayIndex;
   }

   public void setPropertyArrayIndex(int propertyArrayIndex) {
      this.propertyArrayIndex = propertyArrayIndex;
   }

   public void writeAsn(AsnOutput out) {
      out.writeObjectIdentifier(0, this.objectId);
      out.writeEnumerated(1, this.propertyId);
      if (this.propertyArrayIndex != -1) {
         out.writeUnsignedInteger(2, this.propertyArrayIndex);
      }
   }

   public void readAsn(AsnInput in) throws AsnException {
      this.objectId = in.readObjectIdentifier(0);
      this.propertyId = in.readEnumerated(1);
      in.peekTag();
      if (in.isValueTag(2)) {
         this.propertyArrayIndex = in.readUnsignedInt(2);
      } else {
         this.propertyArrayIndex = -1;
      }
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder(32);
      sb.append("\n  " + this.objectId);
      sb.append("\n  " + BBacnetPropertyIdentifier.tag(this.propertyId));
      if (this.propertyArrayIndex != -1) {
         sb.append("[" + this.propertyArrayIndex + "]");
      }

      return sb.toString();
   }

   @Override
   public boolean equals(Object o) {
      return !(o instanceof NBacnetObjectPropertyReference)
         ? false
         : this.objectId == ((NBacnetObjectPropertyReference)o).objectId
            && this.propertyId == ((NBacnetObjectPropertyReference)o).propertyId
            && this.propertyArrayIndex == ((NBacnetObjectPropertyReference)o).propertyArrayIndex;
   }

   @Override
   public int hashCode() {
      return 1;
   }
}
